package keeper.common.borer

import java.time.{Instant, ZoneId}

import scala.concurrent.duration.Duration
import scala.util.Try

import cats.Order
import cats.data.{NonEmptyList, NonEmptySet}

import keeper.common.borer.syntax.all.*

import io.bullet.borer.{Decoder, Encoder, Reader}

trait BaseCodec {

  given Encoder[Instant] = Encoder.forString.contramap(_.toString)
  given Decoder[Instant] = BaseCodec.myInstantDecoder

  given _nelEncoder[A: Encoder]: Encoder[NonEmptyList[A]] =
    Encoder.of[List[A]].contramap(_.toList)

  given _nelDecoder[A: Decoder]: Decoder[NonEmptyList[A]] =
    Decoder
      .of[List[A]]
      .emap(l =>
        NonEmptyList.fromList(l).toRight(s"Empty list found, but not empty expected")
      )

  given _nesEncoder[A: Encoder]: Encoder[NonEmptySet[A]] =
    _nelEncoder.contramap(_.toNonEmptyList)

  given _nesDecoder[A: Decoder: Order]: Decoder[NonEmptySet[A]] =
    _nelDecoder.map(_.toNes)

  given _mapEncoder[A: Encoder, B: Encoder]: Encoder[Map[A, B]] =
    Encoder.of[List[(A, B)]].contramap(_.toList)

  given _mapDecoder[A: Decoder, B: Decoder]: Decoder[Map[A, B]] =
    Decoder.of[List[(A, B)]].map(_.toMap)

  given Encoder[Duration] = Encoder.forString.contramap(_.toString)

  given Encoder[ZoneId] = Encoder.forString.contramap(_.getId())
}

object BaseCodec extends BaseCodec:
  private val myInstantDecoder: Decoder[Instant] =
    new Decoder[Instant]:
      override def read(r: Reader): Instant =
        if (r.hasLong) Instant.ofEpochSecond(r.readLong())
        else {
          parseInstant(r.readString())
            .fold(r.validationFailure, identity)
        }

  private def parseInstant(str: String): Either[String, Instant] =
    Try(Instant.parse(str)).toEither.left
      .map(_.getMessage)
      .orElse(
        str.toLongOption
          .toRight(
            s"Invalid timestamp (either epoch-seconds or iso string): $str"
          )
          .map(Instant.ofEpochSecond)
      )
