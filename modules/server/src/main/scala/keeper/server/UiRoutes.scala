package keeper.server

import cats.data.{Kleisli, OptionT}
import cats.effect.*

import keeper.server.util.*

import org.http4s.*
import org.http4s.dsl.Http4sDsl

final class UiRoutes[F[_]: Async] extends Http4sDsl[F] with MoreHttp4sDsl[F] {

  private val suffixes = List(
    ".css",
    ".eot",
    ".html",
    ".ico",
    ".jpg",
    ".js",
    ".json",
    ".otf",
    ".png",
    ".svg",
    ".ttf",
    ".woff",
    ".woff2",
    ".xml",
    ".yml"
  )

  def appRoutes[F[_]: Async]: HttpRoutes[F] =
    Kleisli {
      case req if req.method == Method.GET =>
        val p = req.pathInfo.segments match
          case Vector() => "/index.html"
          case _        => req.pathInfo.renderString
        val last = req.pathInfo.segments.lastOption.map(_.encoded).getOrElse("")
        val containsUp = req.pathInfo.segments.exists(_.encoded.contains(".."))
        if (
          req.pathInfo.segments.nonEmpty && (containsUp || !suffixes.exists(
            last.endsWith(_)
          ))
        )
          OptionT.pure(Response.notFound[F])
        else
          StaticFile
            .fromResource(
              s"/META-INF/resources/webjars/keeper-webview$p",
              Some(req),
              true
            )
      case _ =>
        OptionT.none
    }

  def routes: HttpRoutes[F] = appRoutes[F]
}
