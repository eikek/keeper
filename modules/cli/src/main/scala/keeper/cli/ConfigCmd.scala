package keeper.cli

import cats.effect.*

import keeper.cli.config.*

import com.monovore.decline.Opts

object ConfigCmd {

  enum SubOpts:
    case ListDefault(cfg: ListDefaultCmd.Options)
    case ShowCurrent(cfg: ShowCurrentCmd.Options)

  private val listOpts: Opts[ListDefaultCmd.Options] =
    Opts.subcommand("list-defaults", "List locations")(ListDefaultCmd.opts)

  private val showOpts: Opts[ShowCurrentCmd.Options] =
    Opts.subcommand("show-current", "Show the currently applied config")(
      ShowCurrentCmd.opts
    )

  val opts: Opts[SubOpts] =
    listOpts
      .map(SubOpts.ListDefault.apply)
      .orElse(showOpts.map(SubOpts.ShowCurrent.apply))

  def apply(cliConfig: CliConfig, opts: SubOpts): IO[ExitCode] =
    opts match {
      case SubOpts.ListDefault(cfg) => ListDefaultCmd(cfg)
      case SubOpts.ShowCurrent(cfg) => ShowCurrentCmd(cliConfig, cfg)
    }
}
