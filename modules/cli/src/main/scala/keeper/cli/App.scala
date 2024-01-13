package keeper.cli

import cats.effect.*

import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp

object App
    extends CommandIOApp(
      name = "keeper",
      header = "Track and manage your bikes + components"
    ):

  private val versionOpts: Opts[VersionCmd.Options] =
    Opts.subcommand("version", "Show version information")(VersionCmd.opts)

  private val configOpts: Opts[ConfigCmd.SubOpts] =
    Opts.subcommand("config", "Show default and applied configuration")(ConfigCmd.opts)

  private val serverOpts: Opts[ServerCmd.SubOpts] =
    Opts.subcommand("server", "Webview server")(ServerCmd.opts)

  val subCommandOpts: Opts[SubCommandOpts] =
    versionOpts
      .map(SubCommandOpts.Version.apply)
      .orElse(serverOpts.map(SubCommandOpts.Server.apply))
      .orElse(configOpts.map(SubCommandOpts.Config.apply))

  def main: Opts[IO[ExitCode]] =
    subCommandOpts.map(run).map(printError)

  def run(opts: SubCommandOpts): IO[ExitCode] =
    CliConfig
      .load[IO]
      .flatTap(setupLogging)
      .flatMap { cliCfg =>
        opts match
          case SubCommandOpts.Version(c) => VersionCmd(c)
          case SubCommandOpts.Server(c)  => ServerCmd(cliCfg, c)
          case SubCommandOpts.Config(c)  => ConfigCmd(cliCfg, c)
      }

  enum SubCommandOpts:
    case Version(opts: VersionCmd.Options)
    case Server(opts: ServerCmd.SubOpts)
    case Config(opts: ConfigCmd.SubOpts)

  private def printError(io: IO[ExitCode]): IO[ExitCode] =
    io.attempt.flatMap {
      case Right(code) => IO.pure(code)
      case Left(ex: CliError) =>
        IO.println(
          s"ERROR ${ex.getMessage}".in(Styles.error)
        ).as(ExitCode.Error)
      case Left(ex) =>
        ex.printStackTrace()
        IO.println(
          s"ERROR ${ex.getClass.getSimpleName}: ${ex.getMessage}".in(Styles.error)
        ).as(ExitCode.Error)
    }

  private def setupLogging(cfg: CliConfig): IO[Unit] = IO {
    scribe.Logger.root.withMinimumLevel(cfg.logging.minimumLevel).replace()
  }

  extension (self: String)
    def in(s: Styles): String =
      if (self.isBlank) ""
      else s"${s.style}$self${Console.RESET}"
