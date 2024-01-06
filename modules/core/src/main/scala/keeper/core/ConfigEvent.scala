package keeper.core

enum ConfigEvent:
  case ComponentAdd(deviceId: DeviceId, componentId: ComponentId)
  case SubComponentAdd(
      deviceId: Option[DeviceId],
      componentId: ComponentId,
      subComponentId: ComponentId
  )
  case ComponentRemove(deviceId: DeviceId, componentId: ComponentId)
  case SubComponentRemove(
      deviceId: Option[DeviceId],
      componentId: ComponentId,
      subComponentId: ComponentId
  )
  case DeviceDrop(deviceId: DeviceId)
  case ComponentDrop(deviceId: Option[DeviceId], componentId: ComponentId)
