package keeper.server.util

import java.time._

import cats.data.ValidatedNel
import cats.parse.{Numbers, Parser as P}
import cats.syntax.all.*

import org.http4s.ParseFailure

final class AtVar(zone: ZoneId) {
  def unapply(
      params: Map[String, collection.Seq[String]]
  ): Option[Option[ValidatedNel[ParseFailure, Instant]]] =
    params
      .get("at")
      .map(_.headOption.map(_.trim).filter(_.nonEmpty))
      .map {
        case Some(str) =>
          AtVar.Parser.timestamp(zone).parseAll(str) match
            case Left(err) => ParseFailure(err.show, "").invalidNel.some
            case Right(ts) => ts.validNel.some

        case None =>
          None
      }
}

object AtVar:
  def apply(zoneId: ZoneId): AtVar = new AtVar(zoneId)

  private object Parser {
    val year: P[Int] =
      (Numbers.nonZeroDigit ~ Numbers.digit.rep(3, 3)).string
        .map(_.toInt)
        .withContext("Invalid year digits")

    val twoDigits = Numbers.digit
      .rep(1, 2)
      .string
      .map(_.toInt)

    val month: P[Int] =
      twoDigits
        .filter(n => 1 <= n && n <= 12)
        .withContext("Invalid month digits")

    val day: P[Int] =
      twoDigits
        .filter(n => 1 <= n && n <= 31)
        .withContext("Invalid day digits")

    val hour: P[Int] =
      twoDigits
        .filter(n => 0 <= n && n <= 23)
        .withContext("Invalid hour digits")

    val minsec: P[Int] =
      twoDigits
        .filter(n => 0 <= n && n <= 59)
        .withContext("Invalid minute or second digits")

    val utc = (P.char('Z') | P.char('z')).as(ZoneOffset.UTC)

    val date = (year ~ (P.char('-') *> month) ~ (P.char('-') *> day)).map {
      case ((y, m), d) => LocalDate.of(y, m, d)
    }

    val time =
      (hour ~ (P.char(':') *> minsec).? ~ (P.char(':') *> minsec).?).map {
        case ((a, b), c) =>
          val min = b.getOrElse(59)
          val sec = c.getOrElse(59)
          LocalTime.of(a, min, sec)
      }

    val epochSeconds: P[Instant] =
      Numbers.signedIntString.map(_.toLong).map(Instant.ofEpochSecond)

    def timestampString(zone: ZoneId): P[Instant] =
      (date ~ (P.char('T') *> time).? ~ utc.?).map { case ((d, t), z) =>
        val time = t.getOrElse(LocalTime.MIDNIGHT.minusSeconds(1))
        val tz = z.getOrElse(zone)
        ZonedDateTime.of(d, time, tz).toInstant
      }

    def timestamp(zone: ZoneId): P[Instant] =
      timestampString(zone).backtrack | epochSeconds
  }
