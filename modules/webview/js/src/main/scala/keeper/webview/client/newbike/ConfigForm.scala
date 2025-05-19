package keeper.webview.client.newbike

import java.time.Instant

import cats.data.{Validated, ValidatedNel}
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlDivElement

import keeper.bikes.data.ComponentType
import keeper.bikes.event.ConfiguredBrake
import keeper.client.KeeperClient
import keeper.client.data.FetchResult
import keeper.common.Lenses.syntax.*
import keeper.core.ComponentId
import keeper.webview.client.icons.ComponentIcon
import keeper.webview.client.shared.{Css, ShowMountedBtn}
import keeper.webview.client.util.Action

import calico.html.io.{*, given}

object ConfigForm {
  private val logger = scribe.cats.io

  def render(
      model: SignallingRef[IO, ConfigModel],
      client: KeeperClient[IO],
      date: IO[ValidatedNel[String, Instant]]
  ): Resource[IO, HtmlDivElement[IO]] =
    fetchData(client, date)(model).toResource >>
      div(
        cls := Css.flexCol,
        div(
          cls := Css.flexRowCenter + Css("mb-2"),
          ShowMountedBtn.render(model.to(ConfigModel.showMountedParts))
        ),
        simpleType(
          ComponentType.Handlebar,
          model,
          id => ConfigModel.handlebar.exist(_.contains(id)),
          ConfigModel.setter(ComponentType.Handlebar)
        ),
        simpleType(
          ComponentType.Stem,
          model,
          id => ConfigModel.stem.exist(_.contains(id)),
          ConfigModel.setter(ComponentType.Stem)
        ),
        simpleType(
          ComponentType.Chain,
          model,
          id => ConfigModel.chain.exist(_.contains(id)),
          ConfigModel.setter(ComponentType.Chain)
        ),
        simpleType(
          ComponentType.CrankSet,
          model,
          id => ConfigModel.crankSet.exist(_.contains(id)),
          ConfigModel.setter(ComponentType.CrankSet)
        ),
        simpleType(
          ComponentType.Fork,
          model,
          id => ConfigModel.forkId.exist(_ == id),
          ConfigModel.setter(ComponentType.Fork),
          Children(
            ConfigModel.forkId.nonEmpty,
            simpleType(
              ComponentType.FrontMudguard,
              model,
              id => ConfigModel.forkMudguard.exist(_.contains(id)),
              id => ConfigModel.forkMudguard.replace(id)
            ),
            simpleType(
              ComponentType.FrontBrake,
              model,
              id => ConfigModel.forkBrakeId.exist(_ == id),
              id =>
                id.map(i =>
                  ConfigModel.forkBrake.modify(br =>
                    br.map(ConfiguredBrake.id.replace(i)).orElse(Some(ConfiguredBrake(i)))
                  )
                ).getOrElse(ConfigModel.forkBrake.replace(None)),
              Children(
                ConfigModel.forkBrakeId.nonEmpty,
                simpleType(
                  ComponentType.BrakePad,
                  model,
                  id => ConfigModel.forkBrakePad.exist(_.contains(id)),
                  id => ConfigModel.forkBrakePad.replace(id)
                )
              )
            )
          )
        ),
        simpleType(
          ComponentType.FrontDerailleur,
          model,
          id => ConfigModel.frontDerailleur.exist(_.contains(id)),
          ConfigModel.setter(ComponentType.FrontDerailleur)
        ),
        simpleType(
          ComponentType.RearDerailleur,
          model,
          id => ConfigModel.rearDerailleur.exist(_.contains(id)),
          ConfigModel.setter(ComponentType.RearDerailleur)
        ),
        simpleType(
          ComponentType.FrontWheel,
          model,
          id => ConfigModel.frontWheelId.exist(_ == id),
          ConfigModel.setter(ComponentType.FrontWheel),
          Children(
            ConfigModel.frontWheelId.nonEmpty,
            simpleType(
              ComponentType.Tire,
              model,
              id => ConfigModel.frontWheelTire.exist(_.contains(id)),
              id => ConfigModel.frontWheelTire.replace(id)
            ),
            simpleType(
              ComponentType.InnerTube,
              model,
              id => ConfigModel.frontWheelTube.exist(_.contains(id)),
              id => ConfigModel.frontWheelTube.replace(id)
            ),
            simpleType(
              ComponentType.BrakeDisc,
              model,
              id => ConfigModel.frontWheelDisc.exist(_.contains(id)),
              id => ConfigModel.frontWheelDisc.replace(id)
            )
          )
        ),
        simpleType(
          ComponentType.RearWheel,
          model,
          id => ConfigModel.rearWheelId.exist(_ == id),
          ConfigModel.setter(ComponentType.RearWheel),
          Children(
            ConfigModel.rearWheel.exist(_.isDefined),
            simpleType(
              ComponentType.Cassette,
              model,
              id => ConfigModel.cassette.exist(_.contains(id)),
              id => ConfigModel.cassette.replace(id)
            ),
            simpleType(
              ComponentType.Tire,
              model,
              id => ConfigModel.rearWheelTire.exist(_.contains(id)),
              id => ConfigModel.rearWheelTire.replace(id)
            ),
            simpleType(
              ComponentType.InnerTube,
              model,
              id => ConfigModel.rearWheelTube.exist(_.contains(id)),
              id => ConfigModel.rearWheelTube.replace(id)
            ),
            simpleType(
              ComponentType.BrakeDisc,
              model,
              id => ConfigModel.rearWheelDisc.exist(_.contains(id)),
              id => ConfigModel.rearWheelDisc.replace(id)
            )
          )
        ),
        simpleType(
          ComponentType.RearBrake,
          model,
          id => ConfigModel.rearBrakeId.exist(_ == id),
          id =>
            id.map(i =>
              ConfigModel.rearBrake.modify(br =>
                br.map(ConfiguredBrake.id.replace(i)).orElse(Some(ConfiguredBrake(i)))
              )
            ).getOrElse(ConfigModel.rearBrake.replace(None)),
          Children(
            ConfigModel.rearBrakeId.nonEmpty,
            simpleType(
              ComponentType.BrakePad,
              model,
              id => ConfigModel.rearBrakePad.exist(_.contains(id)),
              id => ConfigModel.rearBrakePad.replace(id)
            )
          )
        ),
        simpleType(
          ComponentType.Seatpost,
          model,
          id => ConfigModel.seatpost.exist(_.contains(id)),
          ConfigModel.setter(ComponentType.Seatpost)
        ),
        simpleType(
          ComponentType.Saddle,
          model,
          id => ConfigModel.saddle.exist(_.contains(id)),
          ConfigModel.setter(ComponentType.Saddle)
        ),
        simpleType(
          ComponentType.RearMudguard,
          model,
          id => ConfigModel.rearMudguard.exist(_.contains(id)),
          ConfigModel.setter(ComponentType.RearMudguard)
        )
      )

  private val fieldHeaderStyle =
    Css.of(
      Css.flexRowCenter,
      Css("py-2 px-2 mb-2 text-lg rounded-lg"),
      Css("dark:bg-blue-800 bg-blue-300/75 dark:bg-blue/50"),
      Css.textColorViz
    )

  private def simpleType(
      ct: ComponentType,
      model: SignallingRef[IO, ConfigModel],
      isChecked: ComponentId => ConfigModel => Boolean,
      update: Option[ComponentId] => ConfigModel => ConfigModel,
      children: Children = Children(_ => false)
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls <-- model
        .map(ConfigModel.dataAt(ct).get)
        .changes
        .map(list => Css("mt-4").showWhen(list.nonEmpty)),
      div(
        cls := fieldHeaderStyle,
        ComponentIcon(
          ct,
          cls := "w-8 h-8 mx-2"
        ),
        span(ct.name)
      ),
      model.map(m => (ConfigModel.dataAt(ct).get(m), m.showMountedParts)).changes.map {
        case (options, showMounted) =>
          div(
            cls := Css.none,
            options.map(c =>
              div(
                cls := (Css.flexRowCenter + Css("ml-4 py-1"))
                  .showWhen(c.device.isEmpty || showMounted),
                input.withSelf { in =>
                  (
                    typ := "checkbox",
                    cls := Css.radioInput,
                    nameAttr := s"${ct.name}-${c.id}",
                    checked <-- model.map(isChecked(c.id)).changes,
                    onInput --> Action.all(
                      Action.noDefault,
                      Action.eval(
                        in.checked.get.flatMap(flag =>
                          model.update(update(if (flag) c.id.some else None))
                        )
                      )
                    )
                  )
                },
                div(
                  span(c.component.component.name),
                  " (",
                  c.component.brand.name,
                  " ",
                  c.component.product.name,
                  ")"
                ),
                c.device.map { dev =>
                  div(
                    cls := Css(
                      "italic ml-2 dark:text-yellow-600 text-orange-600"
                    ),
                    "Currently configured on bike ",
                    dev.brand.name,
                    " ",
                    dev.device.name,
                    "!"
                  )
                }
              )
            )
          )
      },
      div(
        cls <-- model
          .map(children.active)
          .map(_ && children.nonEmpty)
          .changes
          .map(flag => Css.flexCol + Css("ml-4 px-4").showWhen(flag)),
        children.children.toList.sequence
      )
    )

  private def fetchData(
      client: KeeperClient[IO],
      date: IO[ValidatedNel[String, Instant]]
  )(model: SignallingRef[IO, ConfigModel]) =
    date.flatMap {
      case Validated.Valid(n) =>
        client.getComponentsAt(n).flatMap {
          case FetchResult.Success(data) =>
            model.update(_.setData(data))
          case FetchResult.RequestFailed(err) =>
            logger.error(s"Fetching component data failed: $err")
        }
      case Validated.Invalid(err) =>
        logger.error(s"The date is invalid. Not fetching components: $err")
    }

  final case class Children(
      active: ConfigModel => Boolean,
      children: Resource[IO, HtmlDivElement[IO]]*
  ):
    def nonEmpty: Boolean = children.nonEmpty

}
