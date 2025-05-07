package keeper.bikes.model

import cats.Semigroup
import cats.data.NonEmptyList

import keeper.bikes.data.{BrandId, ComponentType}
import keeper.core.{ComponentId, SchemaCheck}

import io.bullet.borer.Encoder
import io.bullet.borer.derivation.MapBasedCodecs.*

sealed abstract class BikeServiceError(msg: String) extends RuntimeException(msg)

object BikeServiceError:
  final case class InvalidType(
      component: ComponentId,
      provided: ComponentType,
      actual: ComponentType
  )
  object InvalidType:
    given Encoder[InvalidType] = deriveEncoder

  final case class ComponentCircles(circles: Map[ComponentId, Set[ComponentId]])
      extends BikeServiceError(s"Component tree contains circles: $circles")

  final case class ComponentTypeMismatch(problems: NonEmptyList[InvalidType])
      extends BikeServiceError(s"Some components had invalid types specified: $problems")

  final case class ComponentSchemaMismatch(errors: NonEmptyList[SchemaCheck.SchemaError])
      extends BikeServiceError(s"Components are not lining up properly: $errors")

  final case class ComponentResolveError(error: BikesResolveError)
      extends BikeServiceError(
        s"Components could not be resolved against the inventory: $error"
      )

  final case class BrandNotFound(ids: NonEmptyList[BrandId])
      extends BikeServiceError(s"Some brands coulnd not be found: $ids")
  object BrandNotFound:
    def apply(id: BrandId): BrandNotFound = BrandNotFound(NonEmptyList.of(id))
    given Semigroup[BrandNotFound] =
      Semigroup.instance((a, b) => BrandNotFound(a.ids.concatNel(b.ids)))

  given Encoder[BikeServiceError] =
    Encoder.of[Map[String, String]].contramap(err => Map("message" -> err.getMessage))
