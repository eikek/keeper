package keeper.webview.client

import cats.Eq

enum View:
  case Dashboard
  case Components
  case Products
  case Brands
  case NewBike
  case NewMaintenance
  case StravaSetup

object View:
  given Eq[View] = Eq.fromUniversalEquals
