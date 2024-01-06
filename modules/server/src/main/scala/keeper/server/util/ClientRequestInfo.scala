package keeper.server.util

import cats.data.NonEmptyList
import cats.implicits._

import org.http4s._
import org.http4s.headers._
import org.typelevel.ci.CIString

/** Obtain information about the client by inspecting the request. */
object ClientRequestInfo {

  def getBaseUrl[F[_]](req: Request[F]): Option[Uri] =
    for {
      schemes <- NonEmptyList.fromList(getProtocol(req).toList)
      scheme = schemes.toList.mkString(":")
      host <- getHostname(req)
      port = xForwardedPort(req).orElse(uriPort(req)).getOrElse(80)
      hostPort = if (port == 80 || port == 443) host else s"$host:$port"
    } yield Uri.unsafeFromString(s"$scheme://$hostPort")

  def getHostname[F[_]](req: Request[F]): Option[String] =
    xForwardedHost(req)
      .orElse(xForwardedFor(req))
      .orElse(host(req))

  def getProtocol[F[_]](req: Request[F]): Option[String] =
    xForwardedProto(req).orElse(clientConnectionProto(req))

  private def host[F[_]](req: Request[F]): Option[String] =
    req.headers.get[Host].map(_.host)

  private def xForwardedFor[F[_]](req: Request[F]): Option[String] =
    req.headers
      .get[`X-Forwarded-For`]
      .flatMap(_.values.head)
      .map(_.toInetAddress)
      .flatMap(inet => Option(inet.getHostName).orElse(Option(inet.getHostAddress)))

  private def xForwardedHost[F[_]](req: Request[F]): Option[String] =
    req.headers
      .get(CIString("X-Forwarded-Host"))
      .map(_.head.value)

  private def xForwardedProto[F[_]](req: Request[F]): Option[String] =
    req.headers
      .get(CIString("X-Forwarded-Proto"))
      .map(_.head.value)

  private def clientConnectionProto[F[_]](req: Request[F]): Option[String] =
    req.isSecure.map {
      case true  => "https"
      case false => "http"
    }

  private def xForwardedPort[F[_]](req: Request[F]): Option[Int] =
    req.headers
      .get(CIString("X-Forwarded-Port"))
      .map(_.head.value)
      .flatMap(str => Either.catchNonFatal(str.toInt).toOption)

  private def uriPort[F[_]](req: Request[F]): Option[Int] =
    req.serverPort.map(_.value)
}
