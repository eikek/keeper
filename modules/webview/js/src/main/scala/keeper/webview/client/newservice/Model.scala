package keeper.webview.client.newservice

import java.time.ZoneId

import cats.Eq
import cats.data.ValidatedNel
import cats.syntax.all.*

import keeper.bikes.data.{ComponentType, ComponentWithDevice}
import keeper.bikes.model.{Bike, BikeBuilds, BikeService}
import keeper.client.data.RequestFailure
import keeper.common.{Distance, Lenses}
import keeper.core.ComponentId

import monocle.{Iso, Lens, Monocle}

final case class Model(
    submitInProgress: Boolean = false,
    view: Model.View = Model.View.Metadata,
    metadata: MetadataModel = MetadataModel(),
    totals: BikeTotalsModel = BikeTotalsModel(),
    serviceEvents: ServiceEventModel = ServiceEventModel(),
    bikes: BikeBuilds = BikeBuilds.empty,
    preview: Either[RequestFailure, BikeBuilds] = Right(BikeBuilds.empty),
    components: Map[ComponentType, List[ComponentWithDevice]] = Map.empty
):
  def getDate = metadata.dateTimeValidated

  def setComponents(list: List[ComponentWithDevice]): Model =
    copy(components = list.groupBy(_.component.product.productType))

  /** Gets either the preview if available or the current build. The preview doesn't
    * contain total data, which is copied over in this case.
    */
  def recentBuilds: BikeBuilds =
    preview match
      case Left(_)                     => bikes
      case Right(prev) if prev.isEmpty => bikes
      case Right(prev) =>
        prev.copy(bikeTotals = bikes.bikeTotals, componentTotals = bikes.componentTotals)

  def recentBikesValidated: Either[RequestFailure, List[Bike]] =
    preview.map(l => if (l.bikes.isEmpty) bikes.bikes else l.bikes)

  def asBikeService(zoneId: ZoneId): ValidatedNel[String, BikeService] =
    (
      metadata.asBikeService(zoneId),
      totals.totalsValidated,
      serviceEvents.eventsValidated
    ).mapN { (bs, tot, evs) =>
      bs.copy(events = evs, totals = tot)
    }

object Model:
  enum View:
    case Metadata
    case BikeTotals
    case Config

  object View:
    given Eq[View] = Eq.fromUniversalEquals

  val view: Lens[Model, View] =
    Lens[Model, View](_.view)(a => _.copy(view = a))

  val metadata: Lens[Model, MetadataModel] =
    Lens[Model, MetadataModel](_.metadata)(a => _.copy(metadata = a))

  val totals: Lens[Model, BikeTotalsModel] =
    Lens[Model, BikeTotalsModel](_.totals)(a => _.copy(totals = a))

  val serviceEvents: Lens[Model, ServiceEventModel] =
    Lens[Model, ServiceEventModel](_.serviceEvents)(a => _.copy(serviceEvents = a))

  val preview: Lens[Model, Either[RequestFailure, BikeBuilds]] =
    Lens[Model, Either[RequestFailure, BikeBuilds]](_.preview)(a => _.copy(preview = a))

  val bikeBuilds: Lens[Model, BikeBuilds] =
    Lens[Model, BikeBuilds](_.bikes)(a => _.copy(bikes = a))

  val componentTotals: Lens[Model, Map[ComponentId, Distance]] =
    bikeBuilds.andThen(BikeBuilds.componentTotals)

  val bikes: Lens[Model, List[Bike]] =
    bikeBuilds.andThen(BikeBuilds.bikes)

  val components: Lens[Model, Map[ComponentType, List[ComponentWithDevice]]] =
    Lens[Model, Map[ComponentType, List[ComponentWithDevice]]](_.components)(a =>
      _.copy(components = a)
    )

  val componentList: Lens[Model, List[ComponentWithDevice]] =
    components.andThen(
      Iso[Map[ComponentType, List[ComponentWithDevice]], List[ComponentWithDevice]](
        _.values.toList.flatten
      )(
        _.groupBy(_.component.product.productType)
      )
    )

  def componentAt(ct: ComponentType): Lens[Model, List[ComponentWithDevice]] =
    components
      .andThen(
        Monocle.at[Map[ComponentType, List[ComponentWithDevice]], ComponentType, Option[
          List[ComponentWithDevice]
        ]](ct)
      )
      .andThen(Lenses.optionToEmpty[List[ComponentWithDevice]])
