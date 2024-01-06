package keeper.bikes.db

import java.time.Instant

import cats.effect.*
import cats.syntax.all.*

import keeper.bikes.data.*
import keeper.core.TotalOutput

final class BaseDataProvision(inventory: Inventory[IO]) {
  def createAll: IO[BaseData] =
    for {
      brands <- createBrands
      prods <- createProducts(brands)
      comps <- createComponents(prods)
      dev <- createDevices(brands)
    } yield new BaseData(brands, prods, comps, dev)

  def createBrands: IO[BaseData.Brands] =
    (
      loadBrand("Ribble"),
      loadBrand("Campagnolo"),
      loadBrand("Shimano"),
      loadBrand("Continental"),
      loadBrand("Vittoria")
    ).mapN { (ribble, camp, shi, con, vitt) =>
      new BaseData.Brands:
        val ribbleCycles = ribble
        val campa: Brand = camp
        val shimano: Brand = shi
        val continental: Brand = con
        val vittoria: Brand = vitt
    }

  def createProducts(brands: BaseData.Brands): IO[BaseData.Products] =
    (
      makeProduct(brands.campa, ComponentType.Chain, "Chorus 12s"),
      makeProduct(brands.campa, ComponentType.FrontWheel, "Shamal FW"),
      makeProduct(brands.campa, ComponentType.RearWheel, "Shamal RW"),
      makeProduct(brands.shimano, ComponentType.Chain, "Shimano HG99"),
      makeProduct(brands.shimano, ComponentType.Cassette, "UltegraC"),
      makeProduct(brands.campa, ComponentType.Cassette, "Chorus 12s"),
      makeProduct(brands.continental, ComponentType.Tire, "GP5000"),
      makeProduct(brands.continental, ComponentType.Tire, "TerraSpeed"),
      makeProduct(brands.vittoria, ComponentType.Tire, "Corsa 2.0")
    ).mapN { (ct1, shfw, shrw, hg, uc, cc, gp5, ts, c2) =>
      new BaseData.Products:
        val campaChainChorus12s: BikeProduct = ct1
        val campaFrontWheelShamal: BikeProduct = shfw
        val campaRearWheelShamal: BikeProduct = shrw
        val shimanoHGChain: BikeProduct = hg
        val campaCassetteChorus12: BikeProduct = cc
        val shimanoCassetteUltegra: BikeProduct = uc
        val contiGP5K: BikeProduct = gp5
        val vittoriaCorsa2: BikeProduct = c2
        val contiTerraSpeed: BikeProduct = ts
    }

  def createComponents(products: BaseData.Products): IO[BaseData.Components] =
    (
      makeComponent(products.campaChainChorus12s, "CT1"),
      makeComponent(products.campaChainChorus12s, "CT2"),
      makeComponent(products.vittoriaCorsa2, "Corsa2 1"),
      makeComponent(products.vittoriaCorsa2, "Corsa2 2"),
      makeComponent(products.contiGP5K, "GP5K 1"),
      makeComponent(products.contiGP5K, "GP5K 2"),
      makeComponent(products.campaFrontWheelShamal, "Shamal FW 1"),
      makeComponent(products.campaRearWheelShamal, "Shamal RW  1"),
      makeComponent(products.campaCassetteChorus12, "Chorus 12s 1")
    ).mapN { (ct1, ct2, vc1, vc2, gp1, gp2, shf, shr, cc1) =>
      new BaseData.Components:
        val chainCT1: Component = ct1
        val chainCT2: Component = ct2
        val tireCorsa1: Component = vc1
        val tireCorsa2: Component = vc2
        val tireGP51: Component = gp1
        val tireGP52: Component = gp2
        val shamalFW1: Component = shf
        val shamalRW1: Component = shr
        val cassetteChorus1: Component = cc1
    }

  def createDevices(brands: BaseData.Brands): IO[BaseData.Devices] =
    makeDevice(brands.ribbleCycles, "Shimano Bike").map { dev =>
      new BaseData.Devices:
        override def bike1: Device = dev
    }

  private def loadBrand(name: String): IO[Brand] =
    inventory.brands.findBrands(name, 10).compile.toList.map(_.head)

  private def makeProduct(
      brand: Brand,
      pt: ComponentType,
      name: String,
      weight: Option[Weight] = None
  ): IO[BikeProduct] =
    (
      Clock[IO].realTimeInstant,
      inventory.products.storeProduct(NewBikeProduct(brand.id, pt, name, None, weight))
    ).mapN((t, id) => BikeProduct(id, brand.id, pt, name, None, weight, t))

  private def makeComponent(
      product: BikeProduct,
      name: String,
      added: Instant = Instant.EPOCH,
      total: TotalOutput = TotalOutput.zero
  ): IO[Component] =
    (
      Clock[IO].realTimeInstant,
      inventory.components
        .storeComponent(
          NewComponent(product.id, name, None, ComponentState.Active, added, total)
        )
    ).mapN((t, id) =>
      Component(id, product.id, name, None, ComponentState.Active, added, None, total, t)
    )

  private def makeDevice(
      brand: Brand,
      name: String,
      added: Instant = Instant.EPOCH
  ): IO[Device] =
    (
      Clock[IO].realTimeInstant,
      inventory.devices.storeDevice(
        NewDevice(brand.id, name, None, ComponentState.Active, added)
      )
    ).mapN((t, id) =>
      Device(id, brand.id, name, None, ComponentState.Active, added, None, t)
    )
}
