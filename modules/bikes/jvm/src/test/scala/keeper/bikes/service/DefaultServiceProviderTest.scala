package keeper.bikes.service

import java.time.Instant

import cats.effect.IO
import cats.syntax.all.*

import keeper.bikes.db.PostgresTest
import keeper.bikes.event.{ConfiguredFrontWheel, ConfiguredRearWheel, ServiceEvent}
import keeper.bikes.model.BikeService
import keeper.core.DeviceId

import io.bullet.borer.Json
import munit.CatsEffectSuite

class DefaultServiceProviderTest extends CatsEffectSuite with PostgresTest {

  val date = Instant.parse("2023-12-02T15:35:22Z")

  test("store bike service events") {
    bikeShopWithData.use { case (shop, data) =>
      val bikeService = BikeService(
        name = "a service",
        description = None,
        date = date,
        createdAt = None,
        totals = Nil,
        events = List(
          ServiceEvent.NewBikeEvent(
            brandId = data.brands.ribbleCycles.id,
            name = "My new Bike",
            description = None,
            addedAt = date,
            id = DeviceId(-1L),
            frontWheel = ConfiguredFrontWheel(
              id = data.components.shamalFW1.id,
              brakeDisc = None,
              tire = data.components.tireGP51.id.some
            ).some,
            rearWheel = ConfiguredRearWheel(
              id = data.components.shamalRW1.id,
              brakeDisc = None,
              tire = data.components.tireGP52.id.some,
              cassette = None
            ).some,
            chain = data.components.chainCT1.id.some
          )
        )
      )
      println(Json.encode(bikeService).toUtf8String)
      for {
        r <- shop.serviceProvider.processBikeService(bikeService)
        _ <- IO.println(r)
        t <- shop.inventory.getCurrentBikes(Nil)
        _ <- IO.println(t)
      } yield ()
    }
  }
}
