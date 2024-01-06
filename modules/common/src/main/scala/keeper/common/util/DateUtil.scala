package keeper.common.util

import java.time.*
import java.time.temporal.ChronoUnit

object DateUtil {

  def findStartLastMonday(current: ZonedDateTime) = {
    val curDay = current.getDayOfWeek
    val diffDays = curDay.getValue - DayOfWeek.MONDAY.getValue
    current
      .minus(diffDays, ChronoUnit.DAYS)
      .withHour(0)
      .withMinute(0)
      .withSecond(0)
      .truncatedTo(ChronoUnit.SECONDS)
      .toInstant
  }

  def findPreviousWeek(currentTime: ZonedDateTime, back: Int): (Instant, Instant) = {
    val last = findStartLastMonday(currentTime)
    val a = last.minus(Duration.ofDays(7 * math.max(1, back)))
    val b = a.plus(Duration.ofDays(7)).minusSeconds(1)
    (a, b)
  }
}
