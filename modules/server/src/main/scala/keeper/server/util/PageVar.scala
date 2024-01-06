package keeper.server.util

import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.Page

import org.http4s.ParseFailure

object PageVar {
  val defaultLimit = 50
  val defaultOffset = 0L

  val first = Page(defaultLimit, defaultOffset)

  def unapply(
      params: Map[String, collection.Seq[String]]
  ): Option[ValidatedNel[ParseFailure, Page]] = {
    val limit = params
      .get("limit")
      .flatMap(_.headOption)
      .map(parseInt)
      .map(_.map(_.toInt))
      .getOrElse(defaultLimit.validNel)
    val offset = params
      .get("offset")
      .flatMap(_.headOption)
      .map(parseInt)
      .getOrElse(defaultOffset.validNel)

    (limit, offset).mapN(Page.apply).some
  }

  private def parseInt(str: String): ValidatedNel[ParseFailure, Long] =
    str.toLongOption.toValidNel(ParseFailure(s"Invalid integer value: $str", ""))
}
