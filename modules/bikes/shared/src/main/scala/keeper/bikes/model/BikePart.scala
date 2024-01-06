package keeper.bikes.model

import java.time.Instant

import keeper.bikes.data.*
import keeper.core.{ComponentId, TotalOutput}

trait BikePart:
  def id: ComponentId

  def product: ProductWithBrand

  def name: String

  def description: Option[String]

  def state: ComponentState

  def initialTotal: TotalOutput

  def addedAt: Instant

  def createdAt: Instant

  def subParts: Set[BikePart]
