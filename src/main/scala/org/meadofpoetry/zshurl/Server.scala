package org.meadofpoetry.zshurl

import zio._
import zio.logging._
import zio.interop.catz._
import org.http4s.server.blaze.BlazeServerBuilder
import org.meadofpoetry.zshurl.api._
import org.meadofpoetry.zshurl.db._
import org.meadofpoetry.zshurl.config._

object Server extends App {

  val coreService = new api.Core[AppEnv]

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    val process = args match {
      case "migrate"::_ => migrate.provideCustomLayer(db)
      case "cleanup"::_ => cleanup.provideCustomLayer(db)
      case _ => server.provideCustomLayer(env)
    }
    process.exitCode
  }

  val server =
    ZIO
      .runtime[AppEnv]
      .flatMap { implicit runtime =>
        for {
          conf   <- ZIO.access[Has[Config]](_.get)
          result <- BlazeServerBuilder[AppTask](runtime.platform.executor.asEC)
          .bindHttp(conf.port, conf.host)
          .withHttpApp(coreService.service)
          .resource
          .toManagedZIO
          .useForever
          .foldCauseM(
            err => log.error(err.prettyPrint).as(ExitCode.failure),
            _ => ZIO.succeed(ExitCode.success)
          )
        } yield result
      }

  val db = Config.live ++ ZEnv.live >>> DB.live
  val state = db >>> api.State.live
  val logging =
    Logging.console(
      logLevel = LogLevel.Debug,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("zshurl")
  val env = logging ++ db ++ state ++ Config.live

}
