package org.meadofpoetry

import zio._
import zio.logging._
import org.meadofpoetry.zshurl.api.State
import org.meadofpoetry.zshurl.db.DB
import org.meadofpoetry.zshurl.config.Config

package object zshurl {

  type AppEnv = ZEnv with Logging with DB with State with Has[Config]

  type AppTask[A] = RIO[AppEnv, A]

}
