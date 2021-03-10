package org.meadofpoetry.zshurl

import zio._
import zio.blocking.Blocking
import zio.logging.Logging
import zio.clock.Clock
import zio.interop.catz._
import cats.effect.{ Sync, ContextShift, Blocker }
import org.http4s.{ StaticFile, Request, Response, Uri }
import org.http4s.dsl.Http4sDsl
import org.meadofpoetry.zshurl.db._
import org.meadofpoetry.zshurl.config._

package object api {

  type State = Has[State.Service]

  type ApiEnv = Logging with DB with State with Clock with Blocking

  type ApiTask[A] = RIO[ApiEnv, A]

  private[api] val zdsl = Http4sDsl[ApiTask]

  object State {

    final case class Service(id: Ref[Long], uriTemplate: Uri)

    /* Short URL slug is generated via simple 
     mapping `database id -> slug`. Since we don't
     care about this db_id-slug relation being preserved 
     (all we care is slug uniqueness), we will replace
     db call with simple atomic ref read. */
    def live: ZLayer[DB with Has[Config], Throwable, State] = ZLayer.fromEffect {
      for {
        conf    <- ZIO.access[Has[Config]](_.get)
        uri     =  Uri(
          scheme = Some(Uri.Scheme.http),
          authority = Some(Uri.Authority(
            host = Uri.RegName(conf.host),
            port = if (conf.port==80) None else Some(conf.port)
          ))
        )
        initial <- Queries.selectCurrentLinksId
        ref     <- Ref.make(initial)
      } yield Service(ref, uri)
    }
  }

  def staticResource(
    file: String,
    request: Request[ApiTask],
    default: => ApiTask[Response[ApiTask]]
  ) = ZIO.accessM[ApiEnv] { env =>
    val blocker = Blocker.liftExecutionContext(env.get[Blocking.Service].blockingExecutor.asEC)
    StaticFile
      .fromResource[ApiTask](file, blocker, Some(request))
      .getOrElseF(default)
  }

}
