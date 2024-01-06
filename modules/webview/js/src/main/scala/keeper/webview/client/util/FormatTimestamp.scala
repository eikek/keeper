package keeper.webview.client.util

import java.time.*
import java.time.format.DateTimeFormatter

object FormatTimestamp {

  def apply(i: Instant, zone: ZoneId): String =
    DateTimeFormatter.RFC_1123_DATE_TIME.format(i.atZone(zone))

}
