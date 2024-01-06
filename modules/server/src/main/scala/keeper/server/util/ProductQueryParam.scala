package keeper.server.util

import cats.data.ValidatedNel

import keeper.bikes.SimpleQuery

import org.http4s.ParseFailure

object ProductQueryParam {

  def unapply(
      params: Map[String, collection.Seq[String]]
  ): Option[ValidatedNel[ParseFailure, SimpleQuery]] = {
    val query = params.get("q").toSeq.flatten.headOption.getOrElse("")

    PageVar
      .unapply(params)
      .map(_.map(p => SimpleQuery(query, p)))
  }
}
