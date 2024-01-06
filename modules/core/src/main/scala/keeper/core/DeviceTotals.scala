package keeper.core

import io.bullet.borer.{Decoder, Encoder}

final case class DeviceTotals(
    totals: Map[DeviceId, TotalOutput]
) {

  def get(id: DeviceId): Option[TotalOutput] = totals.get(id)

  def getOrZero(id: DeviceId): TotalOutput = totals.getOrElse(id, TotalOutput.zero)
}

object DeviceTotals:
  val empty: DeviceTotals = DeviceTotals(Map.empty)

  def apply(es: (DeviceId, TotalOutput)*): DeviceTotals =
    DeviceTotals(es.toMap)

  given Encoder[DeviceTotals] =
    Encoder.forMap[DeviceId, TotalOutput, Map].contramap(_.totals)

  given Decoder[DeviceTotals] =
    Decoder.forMap[DeviceId, TotalOutput].map(DeviceTotals.apply)
