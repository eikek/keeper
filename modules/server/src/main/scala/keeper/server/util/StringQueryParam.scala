package keeper.server.util

import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher

object StringQueryParam extends OptionalQueryParamDecoderMatcher[String]("q")
