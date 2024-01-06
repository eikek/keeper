package keeper.cli.server

import cats.effect.*
import cats.syntax.all.*

import keeper.cli.{CliConfig, SharedOpts}
import keeper.server.KeeperServer

import com.comcast.ip4s.*
import com.monovore.decline.Opts

object StartCmd extends SharedOpts:

  case class Options(bindHost: Host, bindPort: Port)

  val opts: Opts[Options] = {
    val host = Opts
      .option[Host]("host", "The host address to bind to")
      .withDefault(host"localhost")

    val port = Opts
      .option[Port]("port", "The port to bind to")
      .withDefault(port"8182")

    (host, port).mapN(Options.apply)
  }

  def apply(cliConfig: CliConfig, opts: Options): IO[ExitCode] =
    bikeShop(cliConfig)
      .flatMap { bs =>
        KeeperServer(opts.bindHost, opts.bindPort, bs, cliConfig.timezone)
      }
      .use { server =>
        IO.println(s"Started webview server at ${server.addressIp4s}") *> IO
          .never[ExitCode]
      }
