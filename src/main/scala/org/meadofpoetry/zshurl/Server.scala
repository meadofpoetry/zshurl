package org.meadofpoetry.zshurl

import zio._
import zio.logging._
import zio.system._
import zio.interop.catz._
import org.http4s.server.blaze.BlazeServerBuilder
import org.meadofpoetry.zshurl.api._
import org.meadofpoetry.zshurl.db._
import org.meadofpoetry.zshurl.config._

object Server extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    val process = args match {
      case "migrate"::_ => migrate.provideCustomLayer(db ++ logging)
      case "cleanup"::_ => cleanup.provideCustomLayer(db ++ logging)
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
          result <- BlazeServerBuilder[ApiTask](runtime.platform.executor.asEC)
          .bindHttp(conf.port, conf.host)
          .withHttpApp(Core.service)
          .resource
          .toManagedZIO
          .useForever
          .foldCauseM(
            err => log.error(err.prettyPrint).as(ExitCode.failure),
            _   => ZIO.succeed(ExitCode.success)
          )
        } yield result
      }

  val defaultLogging =
    Logging.console(
      logLevel = LogLevel.Debug,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("zshurl")

  val config = System.live ++ defaultLogging >>> Config.live
  val db = config ++ ZEnv.live >>> DB.live
  val state = config ++ db >>> api.State.live
  val logging = defaultLogging
  val env = logging ++ db ++ state ++ config

}
