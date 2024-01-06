package keeper.common.borer

import scala.util.Try

import io.bullet.borer.*

trait DecoderSyntax:
  extension [A](delegate: Decoder[A])
    def emap[B](f: A => Either[String, B]): Decoder[B] =
      delegate.mapWithReader((r, a) => f(a).fold(r.validationFailure, identity))

    def tmap[B](f: A => Try[B]): Decoder[B] =
      emap(f.andThen(_.toEither.left.map(_.getMessage)))
