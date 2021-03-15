package org.meadofpoetry.zshurl

import zio._
import zio.logging._
import zio.interop.catz._
import org.http4s.server.blaze.BlazeServerBuilder
import org.meadofpoetry.zshurl.api._
import org.meadofpoetry.zshurl.db._
import org.meadofpoetry.zshurl.config._

object Server extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    val process = args match {
      case "migrate"::_ => migrate.provideCustomLayer(logging ++ db)
      case "cleanup"::_ => cleanup.provideCustomLayer(logging ++ db)
      case _ => server.provideCustomLayer(env)
    }
    process
      .catchAllCause(c =>
        ZIO.foreach(c.failures) {
          exn => log.error(exn.getMessage())
        }).provideCustomLayer(logging)
      .exitCode
  }

  val server: ZIO[AppEnv, Throwable, ExitCode] =
    ZIO.access[Has[Config]](_.get).flatMap { conf =>
      ZIO
        .runtime[AppEnv]
        .flatMap { implicit runtime =>
          BlazeServerBuilder[ApiTask](runtime.platform.executor.asEC)
            .bindHttp(conf.port, conf.host)
            .withHttpApp(Core.service)
            .resource
            .toManagedZIO
            .useForever
        }
    }

  val db: ZLayer[ZEnv, Throwable, DB] =
    Config.live ++ ZEnv.live >>> DB.live

  val state: ZLayer[ZEnv, Throwable, State] =
    Config.live ++ db >>> api.State.live

  val logging: ZLayer[ZEnv, Throwable, Logging] =
    Logging.console() ++ Config.live >>> Logging.modifyLoggerM { logger =>
      ZIO.access[Has[Config]](config =>
        logger
          .derive(LogAnnotation.Name(List("zshurl")))
          .derive(LogAnnotation.Level(config.get.logLevel))
      )
    }

  val env = Config.live ++ db ++ state ++ logging

}
