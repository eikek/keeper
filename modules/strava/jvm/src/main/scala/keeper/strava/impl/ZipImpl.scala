package keeper.strava.impl

import java.io.BufferedInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}

import scala.jdk.CollectionConverters._
import scala.util.Using
import scala.util.Using.Releasable

import cats.effect._
import cats.syntax.all._
import fs2.io.file.{Files, Path}
import fs2.{Chunk, Pipe, Stream}

import keeper.strava.impl.Zip.NameFilter

final private class ZipImpl[F[_]: Async: Files](
    tempDir: Option[Path]
) extends Zip[F] {

  private val createTempDir: Resource[F, Path] =
    Files[F].tempDirectory(tempDir, "keeper-zip-", None)

  def zip(chunkSize: Int): Pipe[F, (String, Stream[F, Byte]), Byte] =
    in => ZipImpl.zipJava(chunkSize, in.through(ZipImpl.deduplicate))

  def zipFiles(chunkSize: Int): Pipe[F, (String, Path), Byte] =
    in => ZipImpl.zipJavaPath(chunkSize, in.through(ZipImpl.deduplicate))

  def unzip(
      chunkSize: Int,
      pathFilter: NameFilter,
      targetDir: Option[Path]
  ): Pipe[F, Byte, Path] = { input =>
    Stream
      .resource(Files[F].tempFile(tempDir, "", ".zip", None))
      .evalTap(tempFile => input.through(Files[F].writeAll(tempFile)).compile.drain)
      .through(unzipFiles(chunkSize, pathFilter, _ => targetDir))
  }

  def unzipFiles(
      chunkSize: Int,
      pathFilter: NameFilter,
      targetDir: Path => Option[Path]
  ): Pipe[F, Path, Path] =
    input =>
      for {
        zipArchive <- input
        tempDir <- targetDir(zipArchive)
          .map(Stream.emit)
          .getOrElse(Stream.resource(createTempDir))
        entries <- Stream.eval(Sync[F].blocking {
          ZipImpl.unzipZipFile(zipArchive, tempDir, pathFilter)
        })
        e <- Stream.chunk(entries)
      } yield e
}

object ZipImpl {
  implicit val zipFileReleasable: Releasable[ZipFile] =
    (resource: ZipFile) => resource.close()

  private def unzipZipFile(zip: Path, target: Path, pathFilter: NameFilter): Chunk[Path] =
    Using.resource(new ZipFile(zip.toNioPath.toFile, StandardCharsets.UTF_8)) { zf =>
      Chunk.iterator(
        zf.entries()
          .asScala
          .filter(ze => !ze.getName.endsWith("/"))
          .filter(ze => pathFilter(ze.getName))
          .map { ze =>
            val out = target / ze.getName
            out.parent.map(_.toNioPath).foreach { p =>
              java.nio.file.Files.createDirectories(p)
            }
            Using.resource(java.nio.file.Files.newOutputStream(out.toNioPath)) { fout =>
              zf.getInputStream(ze).transferTo(fout)
              out
            }
          }
      )
    }

  private def deduplicate[F[_]: Sync, A]: Pipe[F, (String, A), (String, A)] = {
    def makeName(name: String, count: Int): String =
      if (count <= 0) name
      else
        name.lastIndexOf('.') match {
          case n if n > 0 =>
            s"${name.substring(0, n)}_$count${name.substring(n)}"
          case _ =>
            s"${name}_$count"
        }

    @annotation.tailrec
    def unique(
        current: Set[String],
        name: String,
        counter: Int
    ): (Set[String], String) = {
      val nextName = makeName(name, counter)
      if (current.contains(nextName))
        unique(current, name, counter + 1)
      else (current + nextName, nextName)
    }

    in =>
      Stream
        .eval(Ref.of[F, Set[String]](Set.empty[String]))
        .flatMap { ref =>
          in.evalMap { element =>
            ref
              .modify(names => unique(names, element._1, 0))
              .map(n => (n, element._2))
          }
        }
  }

  private def zipJava[F[_]: Async](
      chunkSize: Int,
      entries: Stream[F, (String, Stream[F, Byte])]
  ): Stream[F, Byte] =
    fs2.io.readOutputStream(chunkSize) { out =>
      val zip = new ZipOutputStream(out, StandardCharsets.UTF_8)
      val writeEntries =
        entries.evalMap { case (name, bytes) =>
          val javaOut =
            bytes.through(
              fs2.io.writeOutputStream[F](Sync[F].pure(zip), closeAfterUse = false)
            )
          val nextEntry = Sync[F].delay(zip.putNextEntry(new ZipEntry(name)))
          Resource
            .make(nextEntry)(_ => Sync[F].delay(zip.closeEntry()))
            .use(_ => javaOut.compile.drain)
        }
      val closeStream = Sync[F].delay(zip.close())

      writeEntries.onFinalize(closeStream).compile.drain
    }

  private def zipJavaPath[F[_]: Async](
      chunkSize: Int,
      entries: Stream[F, (String, Path)]
  ): Stream[F, Byte] =
    fs2.io.readOutputStream(chunkSize) { out =>
      val zip = new ZipOutputStream(out, StandardCharsets.UTF_8)
      val writeEntries =
        entries.evalMap { case (name, file) =>
          val javaOut = Sync[F].blocking {
            val fin = new BufferedInputStream(
              java.nio.file.Files.newInputStream(file.toNioPath),
              chunkSize
            )
            fin.transferTo(zip)
            fin.close()
          }

          val nextEntry = Sync[F].delay(zip.putNextEntry(new ZipEntry(name)))
          Resource
            .make(nextEntry)(_ => Sync[F].delay(zip.closeEntry()))
            .use(_ => javaOut)
        }
      val closeStream = Sync[F].delay(zip.close())

      writeEntries.onFinalize(closeStream).compile.drain
    }
}
