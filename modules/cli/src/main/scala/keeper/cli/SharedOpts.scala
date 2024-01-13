package keeper.cli

import cats.effect.{IO, Resource}
import cats.syntax.all.*

import keeper.bikes.{BikeShop, Page}

import com.comcast.ip4s.{Host, Port}
import com.monovore.decline.{Argument, Opts}
import org.typelevel.otel4s.trace.Tracer

trait SharedOpts {
  given Tracer[IO] = Tracer.noop[IO]

  given Argument[Host] =
    Argument.from[Host]("host") { str =>
      Host.fromString(str).toValidNel(s"Invalid host: $str")
    }

  given Argument[Port] =
    Argument.from[Port]("port") { str =>
      Port.fromString(str).toValidNel(s"Invalid port: $str")
    }

  val pageOpts: Opts[Page] = {
    val limit = Opts
      .option[Int]("limit", "Maximum number of entries to return")
      .withDefault(Int.MaxValue)
      .validate(s"limit must be > 0")(_ > 0)
    val offset = Opts
      .option[Long]("offset", "How many entries to skip")
      .withDefault(0L)
      .validate(s"offset must be >= 0")(_ >= 0)

    (limit, offset).mapN(Page.apply).withDefault(Page.unlimited)
  }

  val outputFormatOpts: Opts[OutputFormat] = {
    val json = Opts.flag("json", "Print results in JSON").as(OutputFormat.Json)
    val text =
      Opts.flag("text", "Print results in human readable form").as(OutputFormat.Text)

    json.orElse(text).withDefault(OutputFormat.Text)
  }

  def bikeShop(cliConfig: CliConfig): Resource[IO, BikeShop[IO]] =
    BikeShop.resource[IO](cliConfig.asBikeShopConfig)
}

object SharedOpts extends SharedOpts
