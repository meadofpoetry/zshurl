package org.meadofpoetry.zshurl

import zio._
import zio.logging._
import zio.system._
import zio.console._
import zio.interop.catz._
import org.http4s.server.blaze.BlazeServerBuilder
import org.meadofpoetry.zshurl.api._
import org.meadofpoetry.zshurl.db._
import org.meadofpoetry.zshurl.config._

object Server extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    val process = args match {
      case "migrate"::_ => migrate.provideCustomLayer(db ++ defaultLogging)
      case "cleanup"::_ => cleanup.provideCustomLayer(db ++ defaultLogging)
      case _ => server.provideCustomLayer(Config.live)
    }
    process
      .catchAllCause(c =>
        ZIO.foreach(c.failures) {
          exn => log.error(exn.getMessage())
        }).provideCustomLayer(defaultLogging)
      .exitCode
  }

  /*
   * To be able to pass LogLevel to ZLayer[Logger] constructor
   * we need to jump through the hoops here. 
   */
  val server: ZIO[ZEnv with Has[Config], Throwable, ExitCode] =
    ZIO.access[Has[Config]](_.get).flatMap { conf =>
      val logging = makeLogging(conf.logLevel)
      ZIO
        .runtime[AppEnv]
        .flatMap { implicit runtime =>
          BlazeServerBuilder[ApiTask](runtime.platform.executor.asEC)
            .bindHttp(conf.port, conf.host)
            .withHttpApp(Core.service)
            .resource
            .toManagedZIO
            .useForever
            .foldCauseM(
              err => log.error(err.prettyPrint).as(ExitCode.failure),
              _   => ZIO.succeed(ExitCode.success)
            )
        }
        .provideCustomLayer(Config.live ++ logging ++ db ++ state)
    }

  val db: ZLayer[ZEnv, Throwable, DB] =
    Config.live ++ ZEnv.live >>> DB.live

  val state: ZLayer[ZEnv, Throwable, State] =
    Config.live ++ db >>> api.State.live

  def makeLogging(logLevel: LogLevel): ZLayer[ZEnv, Nothing, Logging] =
    Logging.console(
      logLevel,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("zshurl")

  val defaultLogging: ZLayer[ZEnv, Nothing, Logging] =
    Logging.console(
      logLevel = LogLevel.Debug,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("zshurl")

}
