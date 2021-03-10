package org.meadofpoetry

import zio._
import org.meadofpoetry.zshurl.api.ApiEnv
import org.meadofpoetry.zshurl.db.DB
import org.meadofpoetry.zshurl.config.Config

package object zshurl {

  type AppEnv = ApiEnv with Has[Config]

  type AppTask[A] = RIO[AppEnv, A]

}
