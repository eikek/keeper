package keeper.strava.data

import fs2.io.file.Path

import scodec.bits.ByteVector

sealed trait StravaUploadError {
  def file: Path
}

object StravaUploadError {

  final case class Initial(
      errorResponse: ByteVector,
      responseStatus: Option[Int],
      file: Path
  ) extends StravaUploadError

  final case class Processing(
      lastUploadStatus: StravaUploadStatus,
      file: Path
  ) extends StravaUploadError
}
