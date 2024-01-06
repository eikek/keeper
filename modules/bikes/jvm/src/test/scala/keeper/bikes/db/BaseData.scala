package keeper.bikes.db

import keeper.bikes.data._

case class BaseData(
    brands: BaseData.Brands,
    products: BaseData.Products,
    components: BaseData.Components,
    devices: BaseData.Devices
)

object BaseData:
  trait Brands:
    def campa: Brand
    def shimano: Brand
    def continental: Brand
    def vittoria: Brand
    def ribbleCycles: Brand

    def all: List[Brand] = List(campa, shimano, continental, vittoria, ribbleCycles)
    def getById(id: BrandId): Option[Brand] = all.find(_.id == id)

  trait Products:
    def campaChainChorus12s: BikeProduct
    def campaFrontWheelShamal: BikeProduct
    def campaRearWheelShamal: BikeProduct
    def shimanoHGChain: BikeProduct
    def campaCassetteChorus12: BikeProduct
    def shimanoCassetteUltegra: BikeProduct
    def contiGP5K: BikeProduct
    def vittoriaCorsa2: BikeProduct
    def contiTerraSpeed: BikeProduct

    def all: List[BikeProduct] = List(
      contiGP5K,
      contiTerraSpeed,
      vittoriaCorsa2,
      campaCassetteChorus12,
      shimanoCassetteUltegra,
      shimanoHGChain,
      campaRearWheelShamal,
      campaFrontWheelShamal,
      campaChainChorus12s
    )

  trait Components:
    def chainCT1: Component
    def chainCT2: Component
    def cassetteChorus1: Component
    def tireCorsa1: Component
    def tireCorsa2: Component
    def tireGP51: Component
    def tireGP52: Component
    def shamalFW1: Component
    def shamalRW1: Component

  trait Devices:
    def bike1: Device
