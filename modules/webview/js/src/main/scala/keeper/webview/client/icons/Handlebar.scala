package keeper.webview.client.icons

import keeper.webview.client.shared.Css

object Handlebar extends SvgIcon {
  override protected def content(css: Css): String =
    s"""<!-- icon666.com - MILLIONS vector ICONS FREE -->
       |<svg version="1.1"
       |     xmlns="http://www.w3.org/2000/svg"
       |     xmlns:xlink="http://www.w3.org/1999/xlink"
       |     viewBox="0 0 512 512"
       |     class="${css.render}"
       |     xml:space="preserve">
       |  <g>
       |    <g>
       |      <path d="M127.344,145.717c-6.95-34.215-37.265-60.045-73.5-60.045H38.844v150.042v21.175c0,35.424-13.795,68.729-38.844,93.778 l21.212,21.212c30.716-30.715,47.631-71.553,47.631-114.99v-21.175h80.085v42.863c0,81.47,66.281,147.752,147.753,147.752h118.339 v-89.996h-118.34c-31.848,0-57.757-25.909-57.757-57.755v-42.863H512v0v-89.996H127.344z M148.928,205.715H68.843V118.24 c17.46,6.192,29.999,22.882,29.999,42.476v14.999h50.086V205.715z M482.001,205.715H208.925v72.862 c0,48.388,39.366,87.754,87.754,87.754h88.34v29.999h-88.34c-64.93,0-117.754-52.824-117.754-117.753V175.716h303.076V205.715z">
       |      </path>
       |    </g>
       |  </g>
       |</svg>
       |""".stripMargin
}
