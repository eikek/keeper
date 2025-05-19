package keeper.webview.client.shared

trait Styles {
  val mr2: Css = Css("mr-2")
  val relative: Css = Css("relative")
  val flexRow: Css = Css("flex flex-row")
  val flexRowCenter: Css = flexRow + Css("items-center")
  val flexCol: Css = Css("flex flex-col")
  val flexRowLg: Css = Css("flex flex-col lg:flex-row lg:items-center")
  val flexRowMd: Css = Css("flex flex-col md:flex-row md:items-center")
  val flexRowXl: Css = Css("flex flex-col xl:flex-row")
  val hidden: Css = Css("hidden")
  val bgColor: Css = Css("dark:bg-slate-900", "bg-white")
  val grid: Css = Css("grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4")

  val borderT: Css = Css("border-t dark:border-b-slate-600 border-b-gray-200")
  val borderB: Css = Css("border-b dark:border-b-slate-600 border-b-gray-200")
  val borderR: Css = Css("border-r dark:border-r-slate-600 border-r-gray-200")
  val borderL: Css = Css("border-l dark:border-l-slate-600 border-l-gray-200")
  val borderB2: Css = Css("border-b-2 dark:border-b-slate-600 border-b-gray-200")
  val border: Css = Css("border dark:border-slate-600 border-gray-200")
  val divideBorder: Css = Css("dark:divide-slate-600 divide-gray-200")
  val borderBgColor: Css = Css("dark:bg-slate-600 bg-gray-200")
  val hoverBgBorderColor: Css = Css("dark:hover:bg-slate-600 hover:bg-gray-200")

  val textColor: Css = Css("dark:text-slate-600", "text-gray-600")
  val textColorViz: Css = Css(s"dark:text-slate-200", s"text-gray-800")

  val strokeColor: Css = Css("dark:stroke-slate-300", "stroke-gray-700")
  val fillColor: Css = Css("dark:fill-slate-300", "fill-gray-700")

  val firstHeadline: Css = Css("text-3xl font-bold mt-2 mb-3") + textColorViz
  val secondHeadline: Css = Css("text-xl font-bold mt-2 mb-3") + textColorViz

  val tableFixed: Css = Css("table-fixed w-full border-collapse")
  val tableAuto: Css = Css("table-auto w-full border-collapse")
  val tableHead: Css = Css("dark:bg-slate-800 bg-gray-100")
  val tableHeadRow: Css = borderB2 + Css("my-3")
  val tableRow: Css = borderB
  val tableHeadCell: Css = Css("py-2 px-2 text-left")
  val tableHeadCellSm: Css = tableHeadCell + Css("hidden sm:table-cell")
  val tableHeadCellMd: Css = tableHeadCell + Css("hidden md:table-cell")
  val tableRowCell: Css = Css("py-2 ")
  val tableRowCellSm: Css = tableRowCell + Css("hidden sm:table-cell")
  val tableRowCellMd: Css = tableRowCell + Css("hidden md:table-cell")

  val textInput: Css =
    Css(
      "disabled:opacity-75 p-2 dark:bg-slate-800 rounded"
    ) + textColorViz + border + Css("dark:placeholder:text-slate-500") +
      Css("focus:outline-none focus:ring dark:ring-lime-500 ring-blue-500")

  val radioInput: Css =
    Css("p-2 w-5 h-5 dark:bg-slate-800 mr-3") + textColorViz

  val textAreaInput: Css = textInput

  val iconLink: Css = List(
    Css("dark:text-lime-500 text-blue-500"),
    Css("border dark:border-lime-500 border-blue-500"),
    Css("dark:hover:bg-lime-500/25"),
    Css("hover:bg-blue-500/50"),
    Css("px-4 py-2 rounded cursor-pointer")
  ).reduce(_ + _)

  val iconLinkBasic: Css = List(
    Css("dark:text-slate-500 text-gray-500"),
    Css.border,
    Css("dark:hover:bg-slate-500/25"),
    Css("hover:bg-gray-500/50"),
    Css("px-4 py-2 rounded cursor-pointer")
  ).reduce(_ + _)

  val iconLinkRed: Css = List(
    Css("border border-red-600"),
    Css("bg-red-500/25 text-red-600"),
    Css("hover:bg-red-500/50"),
    Css("dark:border-rose-600 dark:text-rose-600"),
    Css("dark:bg-rose-500/50"),
    Css("dark:hover:bg-rose-500/25"),
    Css("px-4 py-2 rounded cursor-pointer")
  ).reduce(_ + _)

  val form: Css = flexCol + border + Css("px-2 py-2")
  val firstFormField: Css = flexCol
  val formField: Css = Css("py-3") + firstFormField
  val selectInput: Css = List(
    Css("block rounded border-0 py-2 px-2 shadow-sm"),
    Css("focus:ring dark:ring-lime-500 ring-blue-500"),
    Css("dark:bg-slate-800 disabled:opacity-75")
  ).reduce(_ + _)

  val inputLabel: Css =
    Css("mb-1 block font-bold text-sm font-medium leading-6") + textColorViz

  val formSubmitButton: Css = List(
    Css("py-2 px-6 rounded text-center focus:ring-2 dark:ring-lime-500 ring-blue-500"),
    Css("dark:bg-lime-700 dark:text-white dark:hover:bg-lime-600"),
    Css("bg-blue-700 text-white hover:bg-blue-600")
  ).reduce(_ + _)
  val formResetButton: Css = List(
    Css("py-2 px-6 rounded text-center focus:ring-2 dark:ring-lime-500 ring-blue-500"),
    Css("dark:bg-slate-600/50 dark:text-white dark:hover:bg-slate-700"),
    Css("bg-gray-800 text-white hover:bg-gray-700"),
    Css.border
  ).reduce(_ + _)

  val errorBorder: Css = Css("border dark:border-rose-500 border-red-500")
  val errorText: Css = Css("dark:text-rose-500 text-red-500")

  val dropDownMenu: Css = List(
    Css(
      "max-h-48 min-h-0 w-full z-10 mx-0.5 absolute left-0 origin-top-right shadow-lg overflow-auto"
    ),
    Css.flexCol,
    Css.borderB,
    Css.borderR,
    Css.borderL,
    Css.bgColor
  ).reduce(_ + _)

  val dropDownMenuEntry: Css =
    Css("py-2 px-2 cursor-pointer dark:hover:bg-slate-600")

  val greenCheckIcon: Css = Css("fa fa-check dark:text-lime-500 text-green-500")
  val diskIcon: Css = Css("fa fa-floppy-disk")
  val loadingSpinner: Css = Css("fa fa-circle-notch fa-spin")
  val uploadIcon: Css = Css("fa fa-upload")

  val blueBoxed = Css(
    "dark:bg-blue-800/50 bg-blue-300/75 rounded-lg"
  )

  val label = List(
    Css("py-0.5 px-1 rounded border"),
    Css(
      "dark:border-lime-500 dark:bg-lime-500/25 dark:text-lime-500"
    ),
    Css("border-blue-500/50 bg-blue-300 text-blue-500")
  ).reduce(_ + _)

  val labelBasic = List(
    Css("py-0.5 px-1 rounded border"),
    Css("dark:border-slate-400 dark:bg-slate-300/25 dark:text-slate-400"),
    Css("border-blue-500 bg-blue-300/50 text-blue-500")
  ).reduce(_ + _)

  val infoText: Css = List(
    Css("px-3 py-3 text-lg rounded-xl"),
    Css(
      "dark:bg-cyan-200/25 bg-blue-200/75"
    ),
    Css("border dark:border-cyan-200 border-blue-200"),
    Css("dark:text-cyan-200 text-blue-600")
  ).reduce(_ + _)
}
