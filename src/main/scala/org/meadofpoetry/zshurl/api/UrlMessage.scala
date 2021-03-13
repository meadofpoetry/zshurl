package org.meadofpoetry.zshurl.api

import org.http4s.Uri
import org.http4s.circe._
import io.circe._
import io.circe.generic.semiauto._

final case class UrlMessage(url: Uri)

object UrlMessage {

  private[api] implicit val encoderUrlMessage: Encoder[UrlMessage] =
    deriveEncoder[UrlMessage]

  private[api] implicit val decoderUrlMessage: Decoder[UrlMessage] =
    deriveDecoder[UrlMessage]

}
