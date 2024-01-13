package keeper.cli.config

import cats.effect.*

import keeper.cli.{ConfigValues, OutputFormat, SharedOpts}

import com.monovore.decline.Opts
import io.bullet.borer.Json

object ListDefaultCmd extends SharedOpts {

  final case class Options(format: OutputFormat)

  val opts: Opts[Options] =
    outputFormatOpts.map(Options.apply)

  def apply(opts: Options): IO[ExitCode] =
    val values = ConfigValues.getAll
    val out = opts.format.fold(
      Json.encode(values).toUtf8String,
      values.toList
        .sortBy(_._1)
        .map { case (key, default) =>
          s"$key=${default.getOrElse("")}"
        }
        .mkString("\n")
    )
    IO.println(out).as(ExitCode.Success)
}
