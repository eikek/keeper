package keeper.strava

import scala.concurrent.duration._

import cats.effect._
import fs2.Stream
import fs2.io.file.Files

import keeper.strava.data.{StravaAccessToken, StravaFileType}

import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits._

object FileUploadTest extends IOApp {
  val token = StravaAccessToken("none")

  override def run(args: List[String]) = {
    val files = Stream
      .range(1, 10)
      .covary[IO]
      .evalMap(_ => Files[IO].createTempFile)
      .evalTap(p =>
        Stream
          .emit("hello")
          .through(fs2.text.utf8.encode)
          .through(Files[IO].writeAll(p))
          .compile
          .drain
      )

    val config = StravaClientConfig.default.copy(apiUrl = uri"http://localhost:8181")
    val strava =
      EmberClientBuilder
        .default[IO]
        .build
        .evalMap(client => StravaClient[IO](config, client))

    strava
      .use { client =>
        files
          .evalTap(f => IO.println(s"Uploading: $f"))
          .evalMap(f =>
            client.uploadFile(
              token,
              None,
              f,
              StravaFileType.Fit,
              "act",
              None,
              false,
              10.seconds,
              callback
            )
          )
          .evalTap(r => IO.println(s"  - $r"))
          .compile
          .drain
      }
      .as(ExitCode.Success)
  }

  def callback(waited: FiniteDuration, attempts: Int): IO[Unit] =
    IO.println(s"  - waited ${waited.toSeconds}s, attempt $attempts")
}
