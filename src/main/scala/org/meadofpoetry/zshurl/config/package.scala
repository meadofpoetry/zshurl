package org.meadofpoetry.zshurl

import zio._
import zio.system._

package object config {

  final case class Config(
    host: String,
    port: Int,
    dbname: String,
    dbuser: String,
    dbpass: Option[String],
    dbhost: Option[String],
    dbport: Option[Int]
  )

  object Config {
    // TODO dealing with errors and bad values
    val live: ZLayer[System, SecurityException, Has[Config]] = ZLayer.fromEffect {
      for {
        host <- envOrElse("ZSHURL_HOST", "localhost")
        port <- env("ZSHURL_PORT").map(_.get.toInt).fold(
          _ => 8080,
          identity
        )
        dbname <- env("ZSHURL_DBNAME").map(_.get)
        dbuser <- env("ZSHURL_DBUSER").map(_.get)
        dbpass <- env("ZSHURL_DBPASS")
        dbhost <- env("ZSHURL_DBHOST")
        dbport <- env("ZSHURL_DBPORT").map(_.map(_.toInt))
      } yield Config(
        host,
        port,
        dbname,
        dbuser,
        dbpass,
        dbhost,
        dbport
      )
    }

  }

}

