package keeper.strava.impl

import cats.effect.*
import fs2.io.file.{Files, Path}
import fs2.{Pipe, Stream}

import keeper.strava.impl.Zip.NameFilter

trait Zip[F[_]] {

  def zip(chunkSize: Int = Zip.defaultChunkSize): Pipe[F, (String, Stream[F, Byte]), Byte]

  def zipFiles(chunkSize: Int = Zip.defaultChunkSize): Pipe[F, (String, Path), Byte]

  def unzip(
      chunkSize: Int = Zip.defaultChunkSize,
      nameFilter: NameFilter = NameFilter.all,
      targetDir: Option[Path] = None
  ): Pipe[F, Byte, Path]

  def unzipFiles(
      chunkSize: Int = Zip.defaultChunkSize,
      nameFilter: NameFilter = NameFilter.all,
      targetDir: Path => Option[Path] = _ => None
  ): Pipe[F, Path, Path]
}

object Zip {
  val defaultChunkSize = 64 * 1024
  type NameFilter = String => Boolean
  object NameFilter {
    val all: NameFilter = _ => true
  }

  def apply[F[_]: Async: Files](tempDir: Option[Path] = None): Zip[F] =
    new ZipImpl[F](tempDir)
}
