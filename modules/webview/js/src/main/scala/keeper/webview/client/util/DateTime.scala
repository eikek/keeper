package keeper.webview.client.util

import java.time._

import cats.syntax.all.*

object DateTime {

  def parseDate(date: String): Either[String, LocalDate] =
    Either.catchNonFatal(LocalDate.parse(date)).left.map(_.getMessage)

  def parseTime(time: String): Either[String, LocalTime] =
    Either.catchNonFatal(LocalTime.parse(time)).left.map(_.getMessage)

  def parseDateTime(date: String, time: String): Either[String, LocalDateTime] =
    for {
      d <- parseDate(date)
      t <- parseTime(time)
    } yield LocalDateTime.of(d, t)

  def atEndOfDay(zoneId: ZoneId)(ld: LocalDate) =
    ld.atStartOfDay(zoneId).plusDays(1).minusMinutes(1)
}
