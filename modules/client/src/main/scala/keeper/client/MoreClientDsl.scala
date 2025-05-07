package keeper.client

import java.time.Instant

import keeper.bikes.data.*
import keeper.core.ComponentId

import com.github.eikek.borer.compats.http4s.BorerEntityJsonCodec
import org.http4s.QueryParamEncoder
import org.http4s.Uri.Path.SegmentEncoder
import org.http4s.client.dsl.Http4sClientDsl

trait MoreClientDsl[F[_]] extends Http4sClientDsl[F] with BorerEntityJsonCodec {

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
