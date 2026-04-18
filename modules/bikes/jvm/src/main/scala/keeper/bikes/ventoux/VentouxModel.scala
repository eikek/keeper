package keeper.bikes.ventoux

import keeper.bikes.data.Device
import keeper.bikes.model.BikeTotal
import keeper.common.Distance

import io.bullet.borer.Decoder
import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs
import io.bullet.borer.derivation.key

object VentouxModel:

  final case class Gear(
      @key("gear_name")
      name: String,
      @key("gear_type")
      gearType: String
  )
  object Gear:
    given Decoder[Gear] = MapBasedCodecs.deriveDecoder

  final case class Stats(
      count: Int,
      totalDistance: Distance
  )
  object Stats:
    given Decoder[Stats] = MapBasedCodecs.deriveDecoder

  final case class GearStats(
      gear: Option[Gear],
      stats: Stats
  )
  object GearStats:
    given Decoder[GearStats] = MapBasedCodecs.deriveDecoder

  final case class StatsResponse(
      byGear: Map[String, GearStats],
      bySport: Map[String, Stats]
  ):
    def toBikeTotal(bikes: List[Device]): List[BikeTotal] =
      val bikeByName = bikes.map(d => d.name.toLowerCase -> d.id).toMap
      byGear.view.flatMap { case (name, stats) =>
        bikeByName
          .get(name.toLowerCase)
          .map(id => BikeTotal(id, stats.stats.totalDistance))
      }.toList

  object StatsResponse:
    given Decoder[StatsResponse] = MapBasedCodecs.deriveDecoder
