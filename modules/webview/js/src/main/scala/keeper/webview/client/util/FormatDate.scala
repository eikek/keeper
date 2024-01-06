package keeper.webview.client.util

import java.time.*
import java.time.format.DateTimeFormatter

object FormatDate {

  def apply(i: Instant, zone: ZoneId): String =
    DateTimeFormatter.ISO_LOCAL_DATE.format(i.atZone(zone))

}
