package keeper.server.util

import keeper.http.borer.BorerEntityCodec.Implicits.*

import io.bullet.borer.*
import org.http4s.*
import org.http4s.headers.`Content-Length`

object ErrorResponse {

  def apply[F[_]](status: Status, message: String): Response[F] =
    create(status, ErrorMsg(message))

  def apply[F[_]](ex: Throwable): Response[F] =
    create(Status.InternalServerError, ErrorMsg(ex))

  private def create[F[_]](status: Status, msg: ErrorMsg): Response[F] = {
    val w = EntityEncoder[F, ErrorMsg]
    val e = w.toEntity(msg)
    val headers =
      w.headers.put(e.length.flatMap(`Content-Length`.fromLong(_).toOption))

    Response(
      status = status,
      body = e.body,
      headers = headers
    )
  }

  final private case class ErrorMsg(msg: String)
  private object ErrorMsg:
    def apply(ex: Throwable): ErrorMsg = ErrorMsg(ex.getMessage)

    given Encoder[ErrorMsg] = Encoder { (w, e) =>
      w.writeMapOpen(1)
      w.writeMapMember("errorMessage", e.msg)
      w.writeMapClose()
    }
}
