package keeper.cli

case class Styles(style: String) {
  def ++(other: Styles): Styles = Styles(style + other.style)
}

object Styles {
  private def frgb(r: Int, g: Int, b: Int): Styles =
    Styles(s"""\u001b[38;2;$r;$g;${b}m""")

  val bold = Styles(Console.BOLD)
  val error = frgb(255, 0, 0) ++ bold
  val headerOne = frgb(0, 230, 0) ++ bold
}
