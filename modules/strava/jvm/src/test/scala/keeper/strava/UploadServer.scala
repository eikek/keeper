package keeper.strava

import cats.effect.*
import cats.effect.std.Random
import cats.syntax.all.*

import keeper.strava.data.{StravaActivityId, StravaUploadId, StravaUploadStatus}

import com.comcast.ip4s.*
import com.github.eikek.borer.compats.http4s.BorerEntityJsonCodec.given
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.multipart.Multipart

object UploadServer extends IOApp {
  override def run(args: List[String]) = {
    val port = args.headOption
      .flatMap(_.toIntOption)
      .flatMap(Port.fromInt)
      .getOrElse(port"8181")

    EmberServerBuilder
      .default[IO]
      .withPort(port)
      .withHost(host"localhost")
      .withHttpApp(uploadRoutes.orNotFound)
      .build
      .useForever
  }

  def uploadRoutes: HttpRoutes[IO] =
    HttpRoutes.of {
      case req @ POST -> Root / "uploads" =>
        for {
          // must consume the request
          mp <- req.as[Multipart[IO]]
          _ <- mp.parts.traverse(_.body).compile.toVector

          bad =
            randomUploadId.flatMap(id =>
              BadRequest(createStatus(id, Left("there was an error")))
            )

          ok =
            randomUploadId
              .map(createStatus(_, Right(None)))
              .flatMap(Ok(_))

          r <- randomChoice(0.6, ok, bad).flatTap(r =>
            IO.println(s"Got upload request: => $r")
          )
        } yield r

      case GET -> Root / "uploads" / LongVar(id) =>
        val uploadId = StravaUploadId(id)
        val done =
          randomActivityId.flatMap(aId => Ok(createStatus(uploadId, Right(Some(aId)))))

        val wait =
          Ok(createStatus(uploadId, Right(None)))

        randomChoice(0.5, done, wait).flatTap(r =>
          IO.println(s"Got upload status request: => $r")
        )
    }

  def randomChoice[A](prob: Double, right: => IO[A], left: => IO[A]): IO[A] =
    Random
      .scalaUtilRandom[IO]
      .flatMap(_.nextDouble)
      .flatMap { n =>
        if (n <= prob) right else left
      }

  def randomLong: IO[Long] =
    Random.scalaUtilRandom[IO].flatMap(_.betweenLong(100, 9999999))

  def randomActivityId: IO[StravaActivityId] =
    randomLong.map(StravaActivityId.apply)

  def randomUploadId: IO[StravaUploadId] =
    randomLong.map(StravaUploadId.apply)

  def createStatus(
      uploadId: StravaUploadId,
      errorOrId: Either[String, Option[StravaActivityId]]
  ): StravaUploadStatus =
    StravaUploadStatus(
      id = uploadId,
      external_id = None,
      error = errorOrId.left.toOption,
      status = "This is some status",
      activity_id = errorOrId.toOption.flatten
    )
}
