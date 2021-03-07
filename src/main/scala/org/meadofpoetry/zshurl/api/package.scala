package org.meadofpoetry.zshurl

import zio._
import org.meadofpoetry.zshurl.db._

package object api {

  type State = Has[Ref[Long]]

  object State {
    def live: ZLayer[DB, Throwable, State] = ZLayer.fromEffect {
      for {
        initial <- Queries.selectCurrentLinksId
        ref     <- Ref.make(initial)
      } yield ref
    }
  }

}
