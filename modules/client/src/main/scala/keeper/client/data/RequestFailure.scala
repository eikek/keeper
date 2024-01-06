package keeper.client.data

import cats.Eq
import cats.data.NonEmptyList

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import org.http4s.ParseFailure

final case class RequestFailure(message: String, errors: List[String] = Nil)
    extends RuntimeException(message)

object RequestFailure {
  def apply(parseFailure: NonEmptyList[ParseFailure]): RequestFailure =
    RequestFailure("The request was invalid", parseFailure.map(_.message).toList)

  implicit val jsonCodec: Codec[RequestFailure] =
    deriveCodec

  given Eq[RequestFailure] = Eq.fromUniversalEquals
}
