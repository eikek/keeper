package keeper.bikes.service

import cats.Id
import cats.data.NonEmptyList
import cats.syntax.all.*

import keeper.bikes.data.*
import keeper.bikes.event.ServiceEvent
import keeper.bikes.event.ServiceEvent.WaxChain.WaxType
import keeper.bikes.model.BikeServiceError
import keeper.bikes.service.ServiceEventBuilder.*
import keeper.core.DeviceBuilder.given
import keeper.core.{DeviceBuild, DeviceBuilder, TypeInfo}

final private class ServiceEventMapper(typeInfo: TypeInfo[Id, ComponentType]) {
  private val b = DeviceBuilder.withTypes(typeInfo)
  private val schemaCheck = ComponentSchema.check(typeInfo.componentType)

  def validateApply(
      current: DeviceBuild,
      events: List[ServiceEvent]
  ): Either[BikeServiceError, (DeviceBuild, List[TranslatedEvent])] =
    val builders = applyEvents(events)
    val (next, evs) = runDeviceBuild(current, builders)
    DeviceBuild
      .hasCircles(next)
      .leftMap(BikeServiceError.ComponentCircles.apply)
      .flatMap(_ =>
        schemaCheck(next).toEither
          .leftMap(errs => BikeServiceError.ComponentSchemaMismatch(errs))
          .map(b => (b, evs))
      )

  private def runDeviceBuild(
      current: DeviceBuild,
      builders: List[(ServiceEvent, ServiceEventBuilder[Id])]
  ) =
    val (last, tes) = builders.foldLeft(current -> List.empty[TranslatedEvent]) {
      case ((state, res), (ev, builder)) =>
        val (nextState, mainEvents) = builder.run(state)
        val te = TranslatedEvent(ev, mainEvents, nextState)
        (nextState, te :: res)
    }
    (last, updateIndex(tes.reverse))

  private def updateIndex(list: List[TranslatedEvent]) =
    list
      .foldLeft((0, List.empty[TranslatedEvent])) { case ((off, res), ev) =>
        val (nextOff, te) = ev.updateIndex(off)
        (nextOff, te :: res)
      }
      ._2

  private def applyEvents(
      events: List[ServiceEvent]
  ): List[(ServiceEvent, ServiceEventBuilder[Id])] =
    events
      .map(
        _.fold(
          ev => ev -> applyBuildEvent(ev).asServiceEventBuilder,
          ev => ev -> applyNonBuildEvent(ev),
          ev =>
            ev -> List(
              // first non-build event to take decision prior modification
              applyNonBuildEvent(ev),
              applyBuildEvent(ev).asServiceEventBuilder
            ).combineAll
        )
      )

  private def applyNonBuildEvent(
      event: ServiceEvent.NonBuildEvent
  ): ServiceEventBuilder[Id] =
    event match
      case ServiceEvent.WaxChain(chains, wt) =>
        val action = wt match
          case WaxType.Hot  => ActionName.HotWax
          case WaxType.Drip => ActionName.DripWax
        ServiceEventBuilder.componentActions[Id](action, chains.toList)

      case ServiceEvent.CeaseComponent(ids, withSubs) =>
        ids.toList.map(ServiceEventBuilder.ceaseComponent[Id](_, withSubs)).combineAll

      case ServiceEvent.CeaseBike(id, withComps) =>
        ServiceEventBuilder.ceaseBike(id, withComps)

      case ServiceEvent.PatchTube(tubes) =>
        ServiceEventBuilder.componentActions(ActionName.Patch, tubes.toSortedSet.toSeq)

      case ServiceEvent.PatchTire(tires) =>
        ServiceEventBuilder.componentActions(ActionName.Patch, tires.toSortedSet.toSeq)

      case ServiceEvent.CleanBike(bikes) =>
        ServiceEventBuilder.bikeAction(ActionName.Clean, bikes.toSortedSet.toSeq)

      case ServiceEvent.CleanComponent(components) =>
        ServiceEventBuilder.componentActions(
          ActionName.Clean,
          components.toSortedSet.toSeq
        )

  private def applyBuildEvent(
      event: ServiceEvent.BuildEvent
  ): DeviceBuilder[Id] =
    event match
      case ServiceEvent.CeaseBike(id, withComps) =>
        DeviceBuilder.dropDevice[Id](id, withComps)

      case ServiceEvent.CeaseComponent(ids, withSubs) =>
        val idList = ids.toList
        (idList.map(DeviceBuilder.removeFromAll[Id](_)) ++
          idList.map(DeviceBuilder.dropComponent[Id](_, withSubs))).combineAll

      case ev: ServiceEvent.ChangeBrakePads =>
        val brakePadPathFront = NonEmptyList.of(
          ComponentType.Fork,
          ComponentType.FrontBrake,
          ComponentType.BrakePad
        )
        val brakePadPathRear = NonEmptyList.of(
          ComponentType.RearBrake,
          ComponentType.BrakePad
        )
        DeviceBuilder.combine(
          ev.front.fold(
            DeviceBuilder.doNothing[Id],
            b.removeSubComponentByTypeN(ev.bike, brakePadPathFront),
            id => b.replaceSubComponentByTypeN(ev.bike, brakePadPathFront, id)
          ),
          ev.rear.fold(
            DeviceBuilder.doNothing[Id],
            b.removeSubComponentByTypeN(ev.bike, brakePadPathRear),
            id => b.replaceSubComponentByTypeN(ev.bike, brakePadPathRear, id)
          )
        )

      case ev: ServiceEvent.ChangeTires =>
        val tirePathFront = NonEmptyList.of(ComponentType.FrontWheel, ComponentType.Tire)
        val tirePathRear = NonEmptyList.of(ComponentType.RearWheel, ComponentType.Tire)
        DeviceBuilder.combine(
          ev.front.fold(
            DeviceBuilder.doNothing[Id],
            b.removeSubComponentByTypeN(ev.bike, tirePathFront),
            id => b.replaceSubComponentByTypeN(ev.bike, tirePathFront, id)
          ),
          ev.rear.fold(
            DeviceBuilder.doNothing[Id],
            b.removeSubComponentByTypeN(ev.bike, tirePathRear),
            id => b.replaceSubComponentByTypeN(ev.bike, tirePathRear, id)
          )
        )

      case ev: ServiceEvent.ChangeFrontWheel =>
        DeviceBuilder.combineAll(ComponentType.values.toList.map { ct =>
          ev.forType(ct)
            .fold(
              DeviceBuilder.doNothing,
              b.removeFromComponentByType(ev.wheel, ct),
              id => b.replaceOnComponent(ev.wheel, id, ct)
            )
        })

      case ev: ServiceEvent.ChangeRearWheel =>
        DeviceBuilder.combineAll(ComponentType.values.toList.map { ct =>
          ev.forType(ct)
            .fold(
              DeviceBuilder.doNothing,
              b.removeFromComponentByType(ev.wheel, ct),
              id => b.replaceOnComponent(ev.wheel, id, ct)
            )
        })

      case ev: ServiceEvent.ChangeFork =>
        DeviceBuilder.combineAll(ComponentType.values.toList.map { ct =>
          ev.forType(ct)
            .fold(
              DeviceBuilder.doNothing,
              b.removeFromComponentByType(ev.fork, ct),
              id => b.replaceOnComponent(ev.fork, id, ct)
            )
        })

      case ev: ServiceEvent.ChangeBike =>
        val bike = ev.bike
        DeviceBuilder.combineAll(ComponentType.values.toList.map { ct =>
          ev.forType(ct)
            .fold(
              DeviceBuilder.doNothing,
              b.removeFromDeviceByType(bike, ct),
              id => b.replaceOnDevice(bike, id, ct)
            )
        })

      case ev: ServiceEvent.NewBikeEvent =>
        val bike = ev.id
        DeviceBuilder.combineAll(ComponentType.values.toList.map {
          case ct @ ComponentType.Fork =>
            ev.fork.map(f => b.replaceOnDevice(bike, f.id, ct)).orEmpty

          case ct @ ComponentType.Chain =>
            ev.chain.map(id => b.replaceOnDevice(bike, id, ct)).orEmpty

          case ct @ ComponentType.RearWheel =>
            ev.rearWheel.map(w => b.replaceOnDevice(bike, w.id, ct)).orEmpty

          case ct @ ComponentType.FrontWheel =>
            ev.frontWheel.map(w => b.replaceOnDevice(bike, w.id, ct)).orEmpty

          case ct @ ComponentType.Handlebar =>
            ev.handlebar.map(id => b.replaceOnDevice(bike, id, ct)).orEmpty

          case ct @ ComponentType.RearMudguard =>
            ev.rearMudguard.map(id => b.replaceOnDevice(bike, id, ct)).orEmpty

          case ct @ ComponentType.FrontMudguard =>
            ev.fork
              .flatMap(fork =>
                fork.mudguard.map(id => b.replaceOnComponent(fork.id, id, ct))
              )
              .orEmpty

          case ct @ ComponentType.BrakeDisc =>
            DeviceBuilder.combine(
              ev.frontWheel
                .flatMap(fw =>
                  fw.brakeDisc.map(id => b.replaceOnComponent(fw.id, id, ct))
                )
                .orEmpty,
              ev.rearWheel
                .flatMap(rw =>
                  rw.brakeDisc.map(id => b.replaceOnComponent(rw.id, id, ct))
                )
                .orEmpty
            )

          case ct @ ComponentType.Tire =>
            DeviceBuilder.combine(
              ev.frontWheel
                .flatMap(fw => fw.tire.map(id => b.replaceOnComponent(fw.id, id, ct)))
                .orEmpty,
              ev.rearWheel
                .flatMap(rw => rw.tire.map(id => b.replaceOnComponent(rw.id, id, ct)))
                .orEmpty
            )

          case ct @ ComponentType.InnerTube =>
            DeviceBuilder.combine(
              ev.frontWheel
                .flatMap(fw => fw.tube.map(id => b.replaceOnComponent(fw.id, id, ct)))
                .orEmpty,
              ev.rearWheel
                .flatMap(rw => rw.tube.map(id => b.replaceOnComponent(rw.id, id, ct)))
                .orEmpty
            )

          case ct @ ComponentType.Cassette =>
            ev.rearWheel
              .flatMap(rw => rw.cassette.map(id => b.replaceOnComponent(rw.id, id, ct)))
              .orEmpty

          case ct @ ComponentType.BrakePad =>
            DeviceBuilder.combine(
              ev.rearBrake
                .flatMap(rb => rb.pad.map(id => b.replaceOnComponent(rb.id, id, ct)))
                .orEmpty,
              ev.fork
                .flatMap(f =>
                  f.brake.flatMap(brake =>
                    brake.pad.map(id => b.replaceOnComponent(brake.id, id, ct))
                  )
                )
                .orEmpty
            )

          case ct @ ComponentType.FrontBrake =>
            ev.fork
              .flatMap(f =>
                f.brake.map(brake => b.replaceOnComponent(f.id, brake.id, ct))
              )
              .orEmpty

          case ct @ ComponentType.RearBrake =>
            ev.rearBrake.map(rb => b.replaceOnDevice(bike, rb.id, ct)).orEmpty

          case ct @ ComponentType.Seatpost =>
            ev.seatpost.map(id => b.replaceOnDevice(bike, id, ct)).orEmpty

          case ct @ ComponentType.Saddle =>
            ev.saddle.map(id => b.replaceOnDevice(bike, id, ct)).orEmpty

          case ct @ ComponentType.FrontDerailleur =>
            ev.frontDerailleur.map(id => b.replaceOnDevice(bike, id, ct)).orEmpty

          case ct @ ComponentType.RearDerailleur =>
            ev.rearDerailleur.map(id => b.replaceOnDevice(bike, id, ct)).orEmpty

          case ct @ ComponentType.Stem =>
            ev.stem.map(id => b.replaceOnDevice(bike, id, ct)).orEmpty
        })
}
