package keeper.webview.client.newservice

import java.time.ZoneId

import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlDivElement

import keeper.bikes.data.{ComponentType, ComponentWithDevice}
import keeper.bikes.event.{ServiceEvent, ServiceEventName}
import keeper.bikes.model.{Bike, BikeBuilds}
import keeper.common.Lenses.syntax.*
import keeper.webview.client.icons._
import keeper.webview.client.shared.Css
import keeper.webview.client.util.{Action, ServiceEventLabel}

import calico.html.io.{*, given}

object ServiceEventForm {

  def render(
      model: SignallingRef[IO, ServiceEventModel],
      builds: Signal[IO, BikeBuilds],
      components: Signal[IO, Map[ComponentType, List[ComponentWithDevice]]],
      zoneId: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := Css.flexCol,
      p(
        cls := Css("text-lg my-2"),
        "Add one or more service events. Check the preview on the right. Once happy, hit 'Submit' above to save."
      ),
      div(
        cls := Css(
          "grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-1 mb-4"
        ),
        selectServiceBtn(
          model,
          ChangeBikeModel(),
          Bicycle,
          "Configure Bike"
        ),
        selectServiceBtn(
          model,
          ChangeWheelModel.frontWheel(),
          Wheel,
          "Configure Front Wheel"
        ),
        selectServiceBtn(
          model,
          ChangeWheelModel.rearWheel(),
          Wheel,
          "Configure Rear Wheel"
        ),
        selectServiceBtn(
          model,
          ChangeTiresModel(),
          Tire,
          "Change Tires"
        ),
        selectServiceBtn(
          model,
          ChangeBrakePadsModel(),
          BrakePad,
          "Change Pads"
        ),
        selectServiceBtn(
          model,
          ChangeForkModel(),
          Fork,
          "Configure Fork"
        ),
        selectServiceBtn(
          model,
          WaxChainModel(),
          Chain,
          "Wax Chain"
        ),
        selectServiceBtn(
          model,
          PatchModel.forTubes,
          InnerTube,
          "Patch Tubes"
        ),
        selectServiceBtn(
          model,
          PatchModel.forTires,
          Tire,
          "Patch Tires"
        ),
        selectServiceBtn(
          model,
          CleanComponentModel(),
          Css("fa fa-soap"),
          "Clean Components"
        ),
        selectServiceBtn(
          model,
          CleanBikeModel(),
          Css("fa fa-soap"),
          "Clean Bikes"
        ),
        selectServiceBtn(
          model,
          CeaseComponentModel(),
          Css("fa fa-skull-crossbones"),
          "Cease Components"
        ),
        selectServiceBtn(
          model,
          CeaseBikeModel(),
          Css("fa fa-skull-crossbones"),
          "Cease Bikes"
        )
      ),
      div(
        cls := Css.flexCol,
        div(
          cls := Css.flexRowLg + Css("items-center mt-4 border-collapse") + Css.border,
          div(
            cls := "grow",
            model
              .map(_.activeForm)
              .changes
              .map(form =>
                form.asServiceEvent.fold(
                  errs => ul(cls := "list-disc list-inside mx-3", errs.toList),
                  _ => ul(cls := Css.hidden)
                )
              )
          ),
          div(
            cls := "flex",
            a(
              href := "#",
              cls := Css.formSubmitButton + Css("my-2 mr-1"),
              onClick --> Action.all(
                Action.eval(addEvent(model))
              ),
              span(cls := "fa fa-plus mr-2"),
              "Add"
            )
          )
        ),
        model.map(_.active).changes.map {
          case ServiceEventName.ChangeBike =>
            ChangeBikeForm.render(
              model.to(ServiceEventModel.changeBike),
              builds,
              components
            )
          case ServiceEventName.ChangeFrontWheel =>
            ChangeWheelForm.renderFrontWheel(
              model.to(ServiceEventModel.changeFrontWheel),
              builds,
              components
            )
          case ServiceEventName.ChangeRearWheel =>
            ChangeWheelForm.renderRearWheel(
              model.to(ServiceEventModel.rearWheel),
              builds,
              components
            )
          case ServiceEventName.ChangeFork =>
            ChangeForkForm.render(
              model.to(ServiceEventModel.fork),
              builds,
              components
            )
          case ServiceEventName.ChangeTires =>
            ChangeTiresForm.render(
              model.to(ServiceEventModel.tires),
              builds,
              components
            )
          case ServiceEventName.ChangeBrakePads =>
            ChangeBrakePadsForm.render(
              model.to(ServiceEventModel.pads),
              builds,
              components
            )
          case ServiceEventName.WaxChain =>
            WaxChainForm.render(
              model.to(ServiceEventModel.waxChain),
              components,
              builds
            )
          case ServiceEventName.PatchTube =>
            PatchForm.render(
              model.to(ServiceEventModel.patchTube),
              ComponentType.InnerTube,
              components,
              builds
            )
          case ServiceEventName.PatchTire =>
            PatchForm.render(
              model.to(ServiceEventModel.patchTire),
              ComponentType.Tire,
              components,
              builds
            )
          case ServiceEventName.CleanComponent =>
            CleanComponentForm.render(
              model.to(ServiceEventModel.cleanComponent),
              components,
              builds,
              zoneId
            )
          case ServiceEventName.CleanBike =>
            CleanBikeForm.render(
              model.to(ServiceEventModel.cleanBike),
              builds.map(_.bikes)
            )
          case ServiceEventName.CeaseComponent =>
            CeaseComponentForm.render(
              model.to(ServiceEventModel.trashComponents),
              components,
              builds,
              zoneId
            )
          case ServiceEventName.CeaseBike =>
            CeaseBikeForm.render(
              model.to(ServiceEventModel.trashBike),
              builds.map(_.bikes)
            )
          case _ =>
            div(cls := Css.hidden)
        },
        (builds, model).mapN((bs, m) => (bs.bikes, m.events)).changes.map {
          case (bs, evs) =>
            div(
              cls := Css.flexCol + Css("my-2 space-y-1"),
              evs.traverse(serviceEventEntry(model, _, bs))
            )
        }
      )
    )

  def serviceEventEntry(
      model: SignallingRef[IO, ServiceEventModel],
      ev: ServiceEvent,
      bikes: List[Bike]
  ) =
    div(
      cls := Css.flexRowCenter + Css(
        "py-2 px-2 text-lg rounded dark:bg-lime-800 dark:text-slate-200 dark:bg-opacity-90"
      ) + Css("bg-blue-500 bg-opacity-75 text-slate-200"),
      div(
        cls := Css("grow") + Css.flexRowCenter,
        div(
          cls := "fa fa-cog mr-2"
        ),
        ServiceEventLabel(ev, id => bikes.find(_.id == id).map(_.name))
      ),
      div(
        a(
          href := "#",
          cls := Css.of(
            Css("h-8 w-8 text-center block shadow-lg"),
            Css("px-2 py-1 rounded cursor-pointer"),
            Css("dark:bg-slate-300 dark:text-lime-800 dark:hover:bg-slate-100"),
            Css("bg-slate-100 text-gray-600 hover:bg-slate-200")
          ),
          onClick --> Action.eval(removeEvent(model, ev)).pipe,
          span(cls := Css("fa fa-xmark"))
        )
      )
    )

  def selectServiceBtn(
      model: SignallingRef[IO, ServiceEventModel],
      newForm: AsServiceEvent,
      icon: SvgIcon | Css,
      label: String
  ) =
    a(
      cls <-- model
        .map(ServiceEventModel.active.get)
        .map(sel =>
          Css.of(
            Css("rounded-lg px-4 py-4 block text-lg"),
            Css.iconLink,
            Css.flexCol,
            Css("items-center"),
            (Css("dark:bg-lime-500 dark:bg-opacity-25") +
              Css("bg-blue-500 bg-opacity-50"))
              .when(sel == newForm.eventName)
          )
        ),
      href := "#",
      onClick --> Action
        .eval(model.update(ServiceEventModel.active.replace(newForm.eventName)))
        .pipe,
      icon match
        case svgIcon: SvgIcon =>
          svgIcon(cls := Css.flexRowCenter + Css("h-10 w-10"))(
            Css("dark:fill-lime-500 fill-blue-500")
          )
        case css: Css =>
          div(cls := css + Css("text-4xl dark:text-lime-500 text-blue-500"))
      ,
      div(cls := Css.flexRowCenter + Css("text-center"), label)
    )

  def addEvent(
      model: SignallingRef[IO, ServiceEventModel]
  ): IO[Unit] =
    model.get.map(_.activeForm).flatMap { fm =>
      fm.asServiceEvent.fold(
        _ => IO.unit,
        se => model.update(ServiceEventModel.prependEvent(se)) >> model.update(fm.reset)
      )
    }

  def removeEvent(model: SignallingRef[IO, ServiceEventModel], ev: ServiceEvent) =
    model.update(ServiceEventModel.removeEvent(ev))
}
