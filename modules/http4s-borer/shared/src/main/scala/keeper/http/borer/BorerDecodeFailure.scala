package keeper.http.borer

import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Borer, Encoder}
import org.http4s._

final case class BorerDecodeFailure(respString: String, error: Borer.Error[?])
    extends DecodeFailure {

  override val message: String = s"${error.getMessage}: $respString"

  override val cause: Option[Throwable] = Option(error.getCause)

  def toHttpResponse[F[_]](httpVersion: HttpVersion): Response[F] =
    Response(status = Status.BadRequest).withEntity(this)
}

object BorerDecodeFailure:
  implicit val borerErrorEncoder: Encoder[Borer.Error[?]] =
    Encoder.forString.contramap(_.getMessage)

  implicit val borerDecodeFailureEncoder: Encoder[BorerDecodeFailure] = deriveEncoder

  implicit def entityEncoder[F[_]]: EntityEncoder[F, BorerDecodeFailure] =
    BorerEntityCodec.encodeEntity[F, BorerDecodeFailure]
