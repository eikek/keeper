package keeper.webview.client

import org.http4s.*

trait BaseUrl {

  def get: Uri

}

object BaseUrl:
  def apply: BaseUrl = new BaseUrlImpl
