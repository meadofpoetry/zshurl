package org.meadofpoetry.zshurl.api

import zio._
import zio.blocking.Blocking
import zio.logging._
import zio.interop.catz._
import cats.effect.Blocker
import io.circe.{ Encoder, Decoder }
import org.http4s.{ StaticFile, Request, Response }
import org.http4s.circe._
import org.http4s.circe.CirceEntityCodec.{ circeEntityDecoder, circeEntityEncoder }

object Http {

  import zdsl._ // Http4sDsl

  def parse[T](req: Request[ApiTask])(implicit d: Decoder[T]): ApiTask[T] =
    req.asJsonDecode[T].onError {
      c => ZIO.foreach(c.failures)(
        err => log.error(err.getMessage())
      )
    }

  def ok[T](data: T)(implicit e: Encoder[T]): ApiTask[Response[ApiTask]] = {
    Ok(data)
  }

  def error(errMsg: String): ApiTask[Response[ApiTask]] = {
    log.error(errMsg) *>
    InternalServerError(errMsg)
  }

  def staticResource(
    file: String,
    request: Request[ApiTask],
    default: => ApiTask[Response[ApiTask]]
  ): ApiTask[Response[ApiTask]] = ZIO.accessM[ApiEnv] { env =>
    val blocker = Blocker.liftExecutionContext(env.get[Blocking.Service].blockingExecutor.asEC)
    StaticFile
      .fromResource[ApiTask](file, blocker, Some(request))
      .getOrElseF(default)
  }

}
