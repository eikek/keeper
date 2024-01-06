package keeper.client

import java.time.Instant

import keeper.bikes.data.*
import keeper.core.ComponentId
import keeper.http.borer.BorerEntityCodec

import org.http4s.QueryParamEncoder
import org.http4s.Uri.Path.SegmentEncoder
import org.http4s.client.dsl.Http4sClientDsl

trait MoreClientDsl[F[_]] extends Http4sClientDsl[F] with BorerEntityCodec.Implicits {

  given SegmentEncoder[ProductId] =
    SegmentEncoder[Long].contramap(_.asLong)

  given SegmentEncoder[ComponentId] =
    SegmentEncoder[Long].contramap(_.asLong)

  given SegmentEncoder[BrandId] =
    SegmentEncoder[Int].contramap(_.asInt)

  given QueryParamEncoder[Instant] =
    QueryParamEncoder[Long].contramap(_.getEpochSecond)

  given QueryParamEncoder[Option[Instant]] =
    QueryParamEncoder[String].contramap(_.map(_.getEpochSecond.toString).getOrElse(""))
}
