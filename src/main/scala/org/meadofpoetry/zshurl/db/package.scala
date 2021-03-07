package org.meadofpoetry.zshurl

import zio._
import zio.logging._
import zio.interop.catz._
import zio.blocking.Blocking
import cats.effect.Blocker
import doobie._
import doobie.hikari._
import doobie.implicits._
import org.meadofpoetry.zshurl.config._

package object db {

  type DB = Has[Transactor[Task]]

  val migrate: ZIO[DB, Throwable, Unit] =
    Migration.up

  val cleanup: ZIO[DB, Throwable, Unit] =
    Migration.down

  object DB {

    val live: ZLayer[Blocking with Has[Config], Throwable, DB] = ZLayer.fromFunctionManaged { env =>
      val bc   = Blocker.liftExecutionContext(env.get.blockingExecutor.asEC)
      val conf = env.get[Config]
      val res  = for {
        ec   <- ExecutionContexts.fixedThreadPool[Task](32)
        xa   <- HikariTransactor.newHikariTransactor[Task](
          "org.postgresql.Driver",     // driver classname
          dbURL(conf),                 // connect URL (driver-specific)
          conf.dbuser,                 // user
          conf.dbpass.getOrElse(""),   // password
          ec,
          bc)
      } yield xa
      res.toManagedZIO
    }

    private def dbURL(conf: Config): String = {
      val path = (conf.dbhost, conf.dbport) match {
        case (Some(host), Some(port)) => s"//$host:$port/${conf.dbname}"
        case (Some(host), _) => s"//$host/${conf.dbname}"
        case _ => conf.dbname
      }
      s"jdbc:postgresql:$path"
    }
  }

}
