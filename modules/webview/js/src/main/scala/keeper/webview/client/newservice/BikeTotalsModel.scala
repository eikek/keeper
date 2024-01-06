package keeper.webview.client.newservice

import cats.Functor
import cats.data.ValidatedNel
import cats.effect.Resource
import cats.syntax.all.*
import fs2.concurrent.SignallingRef

import keeper.bikes.model.BikeTotal
import keeper.common.{Distance, Lenses}
import keeper.core.DeviceId

import monocle.{Lens, Monocle}

final case class BikeTotalsModel(
    loadingData: Boolean = false,
    totals: Map[DeviceId, Distance] = Map.empty,
    inputTotals: Map[DeviceId, String] = Map.empty
):
  def totalsValidated: ValidatedNel[String, List[BikeTotal]] =
    inputTotals.toList
      .traverse { case (id, str) =>
        Distance.fromString(str).toValidatedNel.map(BikeTotal(id, _))
      }

  def isValid: Boolean = totalsValidated.isValid
  def isInvalid: Boolean = !isValid

object BikeTotalsModel:
  val loadingData: Lens[BikeTotalsModel, Boolean] =
    Lens[BikeTotalsModel, Boolean](_.loadingData)(a => _.copy(loadingData = a))

  val totals: Lens[BikeTotalsModel, Map[DeviceId, Distance]] =
    Lens[BikeTotalsModel, Map[DeviceId, Distance]](_.totals)(a =>
      _.copy(totals = a, inputTotals = a.map { case (id, dst) => id -> dst.show })
    )

  val inputTotals: Lens[BikeTotalsModel, Map[DeviceId, String]] =
    Lens[BikeTotalsModel, Map[DeviceId, String]](_.inputTotals)(a =>
      _.copy(inputTotals = a)
    )

  def totalsOf(id: DeviceId) =
    inputTotals
      .andThen(Monocle.at[Map[DeviceId, String], DeviceId, Option[String]](id))
      .andThen(Lenses.emptyString)

  def withLoading[F[_]: Functor](
      model: SignallingRef[F, BikeTotalsModel]
  ): Resource[F, Unit] =
    Resource.make(model.update(loadingData.replace(true)))(_ =>
      model.update(loadingData.replace(false))
    )
