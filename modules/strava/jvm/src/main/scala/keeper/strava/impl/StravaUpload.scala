package keeper.strava.impl

import scala.concurrent.duration.*

import cats.data.EitherT
import cats.effect.*
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}

import keeper.http.borer.BorerEntityCodec
import keeper.http.borer.BorerEntityCodec.Implicits.*
import keeper.strava.StravaClientConfig
import keeper.strava.data.*

import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.`Content-Type`
import org.http4s.multipart.{Multiparts, Part}

final class StravaUpload[F[_]: Async: Files](
    config: StravaClientConfig,
    client: Client[F]
) extends Http4sClientDsl[F] {

  def uploadFile(
      accessToken: StravaAccessToken,
      externalId: Option[String],
      file: Path,
      fileType: StravaFileType,
      name: String,
      description: Option[String],
      commute: Boolean,
      processingTimeout: FiniteDuration,
      waitingCallback: (FiniteDuration, Int) => F[Unit]
  ): F[Either[StravaUploadError, StravaActivityId]] =
    for {
      upload <- initialUpload(
        accessToken,
        externalId,
        file,
        fileType,
        name,
        description,
        commute
      )

      result <-
        EitherT
          .fromEither(upload)
          .flatMapF(uploadStatus =>
            makeResult(
              file,
              accessToken,
              uploadStatus,
              processingTimeout,
              waitingCallback
            )
          )
          .value
    } yield result

  private def makeResult(
      file: Path,
      accessToken: StravaAccessToken,
      initial: StravaUploadStatus,
      timeout: FiniteDuration,
      waitingCallback: (FiniteDuration, Int) => F[Unit]
  ): F[Either[StravaUploadError, StravaActivityId]] = for {
    result <- waitForActivityId(
      accessToken,
      initial,
      timeout,
      waitingCallback
    ).compile.lastOrError
    id = result.activity_id match {
      case Some(aid) => aid.asRight[StravaUploadError]
      case None =>
        Left(StravaUploadError.Processing(result, file))
    }
  } yield id

  private def waitForActivityId(
      accessToken: StravaAccessToken,
      uploadStatus: StravaUploadStatus,
      timeout: FiniteDuration,
      waitingCallback: (FiniteDuration, Int) => F[Unit]
  ): Stream[F, StravaUploadStatus] =
    Stream.eval(Ref.of(0)).flatMap { counter =>
      val count = counter.updateAndGet(_ + 1)

      (Stream.emit(uploadStatus) ++ Stream
        .awakeEvery[F](1.5.seconds)
        .flatMap(fd =>
          Stream.eval(getUploadStatus(accessToken, uploadStatus.id)) ++
            Stream.eval(count.flatMap(c => waitingCallback(fd, c))).drain
        )
        .repeat)
        .takeThrough(s => s.activity_id.isEmpty && s.error.isEmpty)
        .take((timeout / 1.5).toSeconds)
    }

  def initialUpload(
      accessToken: StravaAccessToken,
      externalId: Option[String],
      file: Path,
      fileType: StravaFileType,
      name: String,
      description: Option[String],
      commute: Boolean
  ): F[Either[StravaUploadError, StravaUploadStatus]] = {
    val uri = config.apiUrl / "uploads"
    implicit val errorDecoder: EntityDecoder[F, StravaUploadError] =
      enrichedUploadErrorDecoder(file)

    for {
      mps <- Multiparts.forSync[F]
      body <- mps.multipart(
        Vector(
          Part.formData[F]("name", name).some,
          description.map(d => Part.formData[F]("description", d)),
          Option.when(commute)(Part.formData[F]("commute", "1")),
          Part.formData[F]("data_type", fileType.name).some,
          externalId.map(id => Part.formData[F]("external_id", id)),
          Part
            .fileData(
              "file",
              file,
              `Content-Type`(MediaType.application.`octet-stream`)
            )
            .some
        ).flatten
      )

      req = Method
        .POST(body, uri)
        .putAuth(accessToken)
        .putHeaders(
          `Content-Type`(
            MediaType.multipart.`form-data`
              .withExtensions(Map("boundary" -> body.boundary.value))
          )
        )

      upload <-
        client.expectEither[StravaUploadError, StravaUploadStatus](req)
    } yield upload
  }

  def getUploadStatus(
      accessToken: StravaAccessToken,
      uploadId: StravaUploadId
  ): F[StravaUploadStatus] = {
    val uri = config.apiUrl / "uploads" / uploadId

    client.expect[StravaUploadStatus](
      Method.GET(uri).putAuth(accessToken)
    )
  }

  def enrichedUploadErrorDecoder(file: Path): EntityDecoder[F, StravaUploadError] = {
    val delegate = EntityDecoder.byteVector[F]
    new EntityDecoder[F, StravaUploadError] {
      def decode(m: Media[F], strict: Boolean) = {
        val result = delegate.decode(m, strict)
        val status =
          m match {
            case r: Response[?] => r.status.code.some
            case _              => None
          }

        result.map(bv => StravaUploadError.Initial(bv, status, file))
      }
      def consumes = delegate.consumes
    }
  }
}
