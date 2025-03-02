package keeper.cli

import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.duration.*

import cats.syntax.all.*
import fs2.io.file.Path

import keeper.bikes.db.PostgresConfig
import keeper.bikes.fit4s.Fit4sConfig
import keeper.bikes.strava.StravaConfig
import keeper.cli.config.LoggingConfig
import keeper.common.Password
import keeper.strava.{StravaAppCredentials, StravaClientConfig}

import ciris.*
import com.comcast.ip4s.{Host, Port}
import org.http4s.Uri
import scribe.Level

object ConfigValues {
  private val envPrefix = "KEEPER"
  private val values = new AtomicReference[Map[String, Option[String]]](Map.empty)

  def getAll: Map[String, Option[String]] = values.get()

  lazy val userHome: Path =
    sys.props
      .get("user.home")
      .orElse(sys.env.get("HOME"))
      .map(Path.apply)
      .getOrElse(sys.error(s"No user home directory available!"))

  val postgres = (
    config("POSTGRES_HOST", "localhost").as[Host],
    config("POSTGRES_PORT", "5432").as[Port],
    config("POSTGRES_DATABASE", "keeper"),
    config("POSTGRES_USER"),
    config("POSTGRES_PASSWORD").redacted.as[Password],
    config("POSTGRES_DEBUG", "false").as[Boolean],
    config("POSTGRES_MAX_CONNECTIONS", "8").as[Int]
  ).mapN(PostgresConfig.apply)

  val logging =
    config("LOGGING_LEVEL", "trace")
      .as[Level]
      .map(LoggingConfig.apply)

  val fit4s = {
    val uri = config("FIT4S_URI").as[Uri]
    val timeout = config("FIT4S_TIMEOUT", "60s").as[Duration]
    (uri, timeout).mapN(Fit4sConfig.apply)
  }

  val strava = {
    val defaults = StravaClientConfig.Defaults
    val authUrl =
      config("STRAVA_AUTH_URL", defaults.authUrl.renderString)
        .as[Uri]

    val tokenUrl =
      config("STRAVA_TOKEN_URL", defaults.tokenUrl.renderString)
        .as[Uri]

    val apiUrl =
      config("STRAVA_API_URL", defaults.apiUrl.renderString).as[Uri]

    val clientId = config("STRAVA_CLIENT_ID")
    val clientSecret = config("STRAVA_CLIENT_SECRET")

    val clientConfig = (authUrl, tokenUrl, apiUrl).mapN(StravaClientConfig.apply)

    val credentials =
      (clientId, clientSecret).mapN(StravaAppCredentials.apply)

    val timeout = config("STRAVA_TIMEOUT", "60s").as[Duration]

    (credentials, clientConfig, timeout).mapN(StravaConfig.apply)
  }

  val timeZone =
    config("TIMEZONE", "Europe/Berlin").as[ZoneId]

  given ConfigDecoder[String, ZoneId] =
    ConfigDecoder[String].mapOption("TimeZone") { s =>
      if (ZoneId.getAvailableZoneIds.contains(s)) ZoneId.of(s).some
      else None
    }

  given ConfigDecoder[String, Host] =
    ConfigDecoder[String].mapOption("host")(Host.fromString)

  given ConfigDecoder[String, Port] =
    ConfigDecoder[String].mapOption("port")(Port.fromString)

  given ConfigDecoder[String, Password] =
    ConfigDecoder[String].map(Password.apply)

  given ConfigDecoder[String, Uri] =
    ConfigDecoder[String].mapOption("uri") { s =>
      Uri.fromString(s).toOption
    }

  given ConfigDecoder[String, Level] =
    ConfigDecoder[String].map(_.toLowerCase).mapOption("log-level") {
      case "trace" => Level.Trace.some
      case "debug" => Level.Debug.some
      case "info"  => Level.Info.some
      case "warn"  => Level.Warn.some
      case "error" => Level.Error.some
      case "fatal" => Level.Fatal.some
      case _       => None
    }

  given ConfigDecoder[String, Duration] =
    ConfigDecoder[String].mapOption("duration") { s =>
      Duration.unapply(s).map(Duration.apply.tupled)
    }

  private def addName(name: String, defaultValue: Option[String]) =
    values.updateAndGet(m => m.updated(name, defaultValue))

  private def config(
      name: String,
      default: Option[String]
  ): ConfigValue[Effect, String] = {
    val fullName = s"${envPrefix}_${name.toUpperCase}"
    addName(fullName, default)
    val propName = fullName.toLowerCase.replace('_', '.')
    val cv = prop(propName).or(env(fullName))
    default.map(cv.default(_)).getOrElse(cv)
  }

  private def config(name: String): ConfigValue[Effect, String] = config(name, None)

  private def config(name: String, defval: String): ConfigValue[Effect, String] =
    config(name, Some(defval))
}
