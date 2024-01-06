package keeper.bikes.data

import java.time.Instant

import keeper.core.DeviceId

final case class NewDevice(
    brandId: BrandId,
    name: String,
    description: Option[String],
    state: ComponentState,
    addedAt: Instant
):
  def toDevice(id: DeviceId, createdAt: Instant): Device =
    Device(id, brandId, name, description, state, addedAt, None, createdAt)
