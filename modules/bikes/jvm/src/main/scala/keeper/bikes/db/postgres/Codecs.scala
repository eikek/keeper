package keeper.bikes.db.postgres

import java.nio.charset.StandardCharsets
import java.time.{Duration, Instant, ZoneOffset}

import cats.syntax.all.*

import keeper.bikes.data.*
import keeper.bikes.event.{ServiceEvent, ServiceEventName}
import keeper.bikes.model.{BikeService, BikeServiceError}
import keeper.common.Distance
import keeper.core.{Maintenance as _, *}
import keeper.strava.data._

import io.bullet.borer.{Decoder, Encoder, Json}
import skunk.Codec
import skunk.codec.all as c
import skunk.data.{Arr, Type}

object Codecs {

  val instant: Codec[Instant] =
    c.timestamptz.imap(_.toInstant)(_.atOffset(ZoneOffset.UTC))

  val javaDuration: Codec[Duration] =
    c.int8.imap(Duration.ofSeconds)(_.toSeconds)

  val brandId: Codec[BrandId] = c.int4.imap(BrandId(_))(_.asInt)
  val productId: Codec[ProductId] = c.int8.imap(ProductId(_))(_.asLong)
  val componentId: Codec[ComponentId] = c.int8.imap(ComponentId(_))(_.asLong)
  val maintenanceId: Codec[MaintenanceId] = c.int8.imap(MaintenanceId(_))(_.asLong)
  val deviceId: Codec[DeviceId] = c.int8.imap(DeviceId(_))(_.asLong)
  val maintenanceEventId: Codec[MaintenanceEventId] =
    c.int8.imap(MaintenanceEventId(_))(_.asLong)
  val serviceEventId: Codec[ServiceEventId] =
    c.int8.imap(ServiceEventId(_))(_.asLong)

  val actionId: Codec[ActionId] = c.int8.imap(ActionId(_))(_.asLong)

  val componentType: Codec[ComponentType] =
    c.varchar.eimap(ComponentType.fromString)(_.name.toLowerCase)

  val componentState: Codec[ComponentState] =
    c.varchar.eimap(ComponentState.fromString)(_.name.toLowerCase)

  val totalOutput: Codec[TotalOutput] = c.float8.imap(TotalOutput(_))(_.asDouble)
  val distance: Codec[Distance] = c.float8.imap(Distance.meter)(_.toMeter)

  val serviceEventName: Codec[ServiceEventName] =
    c.varchar.eimap(ServiceEventName.fromName)(_.name)

  val serviceEventJson: Codec[ServiceEvent] = jsonb[ServiceEvent]

  val weight: Codec[Weight] = c.float8.imap(Weight.gramm)(_.toGramm)

  val brand: Codec[Brand] =
    (brandId *: c.varchar *: c.text.opt *: instant).to[Brand]

  val bikeProduct: Codec[BikeProduct] =
    (productId *: brandId *: componentType *: c.varchar *: c.text.opt *: weight.opt *: instant)
      .to[BikeProduct]

  val component: Codec[Component] =
    (componentId *: productId *: c.varchar *: c.text.opt *: componentState *: instant *: instant.opt *: totalOutput *: instant)
      .to[Component]

  val productWithBrand: Codec[ProductWithBrand] =
    (bikeProduct *: brand).to[ProductWithBrand]

  val componentWithProduct: Codec[ComponentWithProduct] =
    (bikeProduct *: brand *: component).to[ComponentWithProduct]

  val actionName: Codec[ActionName] =
    c.varchar.eimap(ActionName.fromString)(_.asString)

  val maintenanceEvent: Codec[MaintenanceEvent] =
    (maintenanceEventId *: maintenanceId *: c.int4 *: actionName *: deviceId.opt *: componentId.opt *: componentId.opt *: instant)
      .to[MaintenanceEvent]

  val device: Codec[Device] =
    (deviceId *: brandId *: c.varchar *: c.text.opt *: componentState *: instant *: instant.opt *: instant)
      .to[Device]

  val deviceWithBrand: Codec[DeviceWithBrand] =
    (device *: brand).to[DeviceWithBrand]

  val bikeServiceErrorInvalidType: Codec[BikeServiceError.InvalidType] =
    (componentId *: componentType *: componentType).to[BikeServiceError.InvalidType]

  def jsonb[A: Encoder: Decoder]: Codec[A] =
    Codec.simple(
      a => Json.encode(a).toUtf8String,
      s =>
        Json
          .decode(s.getBytes(StandardCharsets.UTF_8))
          .to[A]
          .valueEither
          .left
          .map(_.getMessage),
      Type.jsonb
    )

  val bikeServiceMain: Codec[BikeService] =
    (c.varchar *: c.text.opt *: instant *: instant.opt)
      .imap(BikeService(_, _, _, _, Nil, Nil))(bs =>
        bs.name *: bs.description *: bs.date *: bs.createdAt *: EmptyTuple
      )

  val actionNameArr: Codec[Arr[ActionName]] =
    c._varchar.eimap(names => names.traverse(ActionName.fromString))(_.map(_.asString))

  val serviceEventNameArr: Codec[Arr[ServiceEventName]] =
    c._varchar.eimap(_.traverse(ServiceEventName.fromName))(_.map(_.name))

  val serviceEventNameSet: Codec[Set[ServiceEventName]] =
    serviceEventNameArr.imap(_.toList.toSet)(e => Arr(e.toList: _*))

  val stravaAccessToken: Codec[StravaAccessToken] =
    c.text.imap(StravaAccessToken.apply)(_.token)

  val stravaRefreshToken: Codec[StravaRefreshToken] =
    c.text.imap(StravaRefreshToken.apply)(_.token)

  val stravaScopes: Codec[StravaScope] =
    c.text.imap(StravaScope.apply)(_.asString)

  val stravaTokenAndScope: Codec[TokenAndScope] = {
    val respCodec =
      (c.varchar *: stravaAccessToken *: instant *: javaDuration *: stravaRefreshToken)
        .to[StravaTokenResponse]

    (respCodec *: stravaScopes).to[TokenAndScope]
  }

}
