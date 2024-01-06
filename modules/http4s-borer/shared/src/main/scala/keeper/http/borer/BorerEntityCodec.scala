package keeper.http.borer

import cats.data.EitherT
import cats.effect.*
import cats.syntax.all.*
import fs2.Chunk

import io.bullet.borer.*
import org.http4s.*
import org.http4s.headers.*

object BorerEntityCodec:
  def decodeEntity[F[_]: Sync, A: Decoder]: EntityDecoder[F, A] =
    EntityDecoder.decodeBy(MediaType.application.json)(decodeJson)

  def decodeJson[F[_]: Sync, A: Decoder](media: Media[F]): DecodeResult[F, A] =
    EitherT(StreamProvider(media.body).flatMap { implicit input =>
      for {
        bodyString <- media.bodyText.compile.string
        res <- Sync[F].delay(Json.decode(input).to[A].valueEither)
      } yield res.left.map(BorerDecodeFailure(bodyString, _))
    })

  def encodeEntity[F[_], A: Encoder]: EntityEncoder[F, A] =
    EntityEncoder.simple(`Content-Type`(MediaType.application.json))(a =>
      Chunk.array(Json.encode(a).toByteArray)
    )

  trait Implicits:
    implicit def entityDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A] =
      decodeEntity[F, A]
    implicit def entityEncoder[F[_], A: Encoder]: EntityEncoder[F, A] = encodeEntity[F, A]

  object Implicits extends Implicits
