package keeper.cli

import cats.effect.*

import keeper.BuildInfo

import com.monovore.decline.Opts

object VersionCmd extends SharedOpts:

  case class Options(json: Boolean)

  val opts: Opts[Options] =
    Opts(Options(true))

  def apply(opts: Options): IO[ExitCode] =
    IO.println(if (opts.json) showJson else showText).as(ExitCode.Success)

  def showText: String = {
    val name = BuildInfo.name
    val version = BuildInfo.version
    val commit = BuildInfo.gitHeadCommit
      .map(_.take(8))
      .map(c => s" (#$c)")
      .getOrElse("")
    val built = BuildInfo.builtAtString

    s"$name $version, $built$commit"
  }

  def showJson: String =
    toJsonValue(BuildInfo.toMap)

  private def quote(x: scala.Any): String = "\"" + x + "\""

  private def toJsonValue(value: scala.Any): String =
    value match {
      case elem: scala.collection.Seq[?] => elem.map(toJsonValue).mkString("[", ",", "]")
      case elem: scala.Option[?]         => elem.map(toJsonValue).getOrElse("null")
      case elem: scala.collection.Map[?, scala.Any] =>
        elem
          .map { case (k, v) =>
            toJsonValue(k.toString) + ":" + toJsonValue(v)
          }
          .mkString("{", ", ", "}")
      case d: scala.Double     => d.toString
      case f: scala.Float      => f.toString
      case l: scala.Long       => l.toString
      case i: scala.Int        => i.toString
      case s: scala.Short      => s.toString
      case bool: scala.Boolean => bool.toString
      case str: String         => quote(str)
      case other               => quote(other.toString)
    }
