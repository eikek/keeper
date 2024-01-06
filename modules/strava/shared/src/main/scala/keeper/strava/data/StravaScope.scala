package keeper.strava.data

import io.bullet.borer._

final class StravaScope(raw: String) {
  val scopes = raw.split(',').map(_.trim.toLowerCase).filter(_.nonEmpty).toSet

  def hasActivityRead: Boolean = scopes.contains("activity:read")

  def hasActivityWrite: Boolean = scopes.contains("activity:write")

  def asString = scopes.mkString(",")

  override def toString = s"Scopes($asString)"
}
object StravaScope {
  val activityReadAndWrite =
    StravaScope("activity:read,activity:read_all,activity:write,profile:read_all")

  val activityRead =
    StravaScope("activity:read,activity:read_all,profile:read_all")

  def apply(scopes: String): StravaScope = new StravaScope(scopes)

  given Decoder[StravaScope] = Decoder.forString.map(apply)
  given Encoder[StravaScope] = Encoder.forString.contramap(_.asString)
}
