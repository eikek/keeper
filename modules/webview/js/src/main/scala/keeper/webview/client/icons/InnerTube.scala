package keeper.webview.client.icons
import keeper.webview.client.shared.Css

object InnerTube extends SvgIcon {
  override protected def content(css: Css): String =
    s"""<!-- icon666.com - MILLIONS vector ICONS FREE -->
       |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" class="${css.render}">
       |  <g id="outline">
       |    <path d="M46,30H38V27a1,1,0,0,0-1-1H35V16h1a1,1,0,0,0,1-1.083l-.751-9.011a4.26,4.26,0,0,0-8.49,0L27,14.917A1,1,0,0,0,28,16h1V26H27a1,1,0,0,0-1,1v3H18a16,16,0,0,0,0,32H46a16,16,0,0,0,0-32ZM29.747,6.072a2.261,2.261,0,0,1,4.506,0L34.58,10H31v2h3.747l.166,2H29.087ZM31,22V20h2v2Zm2,2v2H31V24Zm-2-6V16h2v2Zm5,30H18a2,2,0,0,1,0-4H36Zm0-6H18a4,4,0,0,0,0,8H36v2H18a6,6,0,0,1,0-12H36Zm0-4H18a8,8,0,0,0,0,16H36v2H18a10,10,0,0,1,0-20H36Zm0-4H18a12,12,0,0,0,0,24H36v2H18a14,14,0,0,1,0-28H36Zm0-4H28V28h8Zm4,30H38V32h2Zm2-16h4a2,2,0,0,1,0,4H42Zm0,6h4a4,4,0,0,0,0-8H42V40h4a6,6,0,0,1,0,12H42Zm0,4h4a8,8,0,0,0,0-16H42V36h4a10,10,0,0,1,0,20H42Zm4,6H42V58h4a12,12,0,0,0,0-24H42V32h4a14,14,0,0,1,0,28Z">
       |    </path>
       |  </g>
       |</svg>
       |""".stripMargin
}
