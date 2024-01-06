package keeper.common.util

object StringCase {

  def camelToSpace(str: String): String =
    splitCamelCase(str).mkString(" ")

  private def splitCamelCase(str: String): List[String] = {
    @annotation.tailrec
    def go(s: String, parts: List[String]): List[String] =
      if (s.isEmpty) parts
      else {
        val c0 = s.charAt(0)
        val cm = s.drop(1).takeWhile(!_.isUpper)
        go(s.drop(cm.length + 1), s"$c0$cm" :: parts)
      }
    go(str, Nil).reverse
  }
}
