package org.meadofpoetry.zshurl

import zio._
import zio.system._
import zio.logging._

package object config {

 // case class RequiredVariables(vars: List[String])
 //     extends Exception("Following variables are required but not set: " + vars.mkString(", "))

  final case class Config(
    logLevel: LogLevel,
    host: String,
    port: Int,
    dbname: String,
    dbuser: String,
    dbpass: Option[String],
    dbhost: Option[String],
    dbport: Option[Int]
  )

  object Config {

    case class RequiredVariables(vars: List[String])
        extends Exception("Following variables are required but not set: " +
          vars.mkString(", "))

    val live: ZLayer[System, Throwable, Has[Config]] = ZLayer.fromEffect {
      val vhost = envWithDefault("ZSHURL_HOST")(Some(_), "localhost")
      val vport = envWithDefault("ZSHURL_PORT")(_.toIntOption, 8080)
      val vdbname = envWithDefault("ZSHURL_DBNAME")(Some(_), "shurl")
      val vdbuser = envRequired("ZSHURL_DBUSER", "USER")(Some(_))
      val vlogLevel = envWithDefault("ZSHURL_LOG_LEVEL")(logLevelFromString, LogLevel.Warn)
      vhost.validate(vport.validate(vdbname.validate(vdbuser.validate(vlogLevel))))
        .mapErrorCause(c => Cause.fail(RequiredVariables(c.failures)))
        .flatMap {
          case (host, (port, (dbname, (dbuser, logLevel)))) =>
            for {
              dbpass <- env("ZSHURL_DBPASS")
              dbhost <- env("ZSHURL_DBHOST")
              dbport <- env("ZSHURL_DBPORT").map(_.map(_.toInt))
            } yield Config(
              logLevel,
              host,
              port,
              dbname,
              dbuser,
              dbpass,
              dbhost,
              dbport
            )
        }
    }

    private def envWithDefault[A](v: String)(conv: String => Option[A], default: => A): ZIO[System, String, A] =
      env(v).catchAll(exn => ZIO.fail(exn.toString())).map(_.flatMap(conv) match {
        case None => default
        case Some(x) => x
      })

    private def envRequired[A](v: String)(conv: String => Option[A]): ZIO[System, String, A] =
      env(v).foldM(
        _ => ZIO.fail(v),
        s => s.flatMap(conv) match {
          case Some(x) => ZIO.succeed(x)
          case None => ZIO.fail(v)
        }
      )

    private def envRequired[A](first: String, rest: String*)(conv: String => Option[A]): ZIO[System, String, A] = {
      def firstSuccess(l: Seq[String]): ZIO[System, String, A] =
        l match {
          case Nil => ZIO.fail(first)
          case v+:tl =>
            env(v).foldM(
              _ => firstSuccess(tl),
              s => s.flatMap(conv) match {
                case Some(x) => ZIO.succeed(x)
                case None => firstSuccess(tl)
              }
            )
        }
      firstSuccess(first +: rest)
    }

    private def logLevelFromString(s: String): Option[LogLevel] = {
      import org.http4s.util.{ CaseInsensitiveString => CIString }
      // import org.typelevel.ci.CIString
      val cis = CIString(s)
      cis match {
        case n if n == CIString("fatal") => Some(LogLevel.Fatal)
        case n if n == CIString("error") => Some(LogLevel.Error)
        case n if n == CIString("warn") => Some(LogLevel.Warn)
        case n if n == CIString("info") => Some(LogLevel.Info)
        case n if n == CIString("debug") => Some(LogLevel.Debug)
        case n if n == CIString("trace") => Some(LogLevel.Trace)
        case n if n == CIString("off") => Some(LogLevel.Off)
        case _ => None
      }
    }

  }

}

