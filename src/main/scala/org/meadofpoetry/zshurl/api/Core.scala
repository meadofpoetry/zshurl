package org.meadofpoetry.zshurl.api

import org.http4s.{ HttpRoutes, StaticFile, Request, Response, Status, Uri }
import org.http4s.headers.Location
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import cats.effect.Blocker
import cats.data.Kleisli
import zio._
import zio.blocking.Blocking
import zio.clock._
import zio.interop.catz._
import zio.logging._
import org.meadofpoetry.zshurl.db._

class Core[R <: Logging with DB with State with Clock with Blocking] {

  type LoggingTask[A] = RIO[R, A]
  private val zdsl = Http4sDsl[LoggingTask]
  import zdsl._

  val service = HttpRoutes.of[LoggingTask] {
    case request @ POST -> Root / "api" / "shortify" =>
      for {
        state <- ZIO.access[State](_.get)
        url64 <- request.bodyAsText.compile.string
        url   = new String(java.util.Base64.getDecoder.decode(url64))
        _     <- log.info("URI: " + url)
        id    <- state.updateAndGet(_ + 1) // TODO
        slug  = genSlug(id)
        time  <- currentDateTime
        _     <- log.info("Request: " + request.uri)
        _     <- Queries.insertLink(url, slug, time).foldCauseM(
          err => log.error(err.prettyPrint),
          _ => ZIO.succeed(())
        )
        res   <- Ok(java.util.Base64.getEncoder.encode(request.uri.withPath(slug).toString().getBytes()))
      } yield res

    case GET -> Root =>
      ZIO.access[Blocking](_.get.blockingExecutor.asEC)
        .flatMap { bc =>
          val blocker = Blocker.liftExecutionContext(bc)
          StaticFile
            .fromResource[LoggingTask]("index.html", blocker, None)
            .getOrElseF(NotFound())
        }

    case GET -> Root / "js" / "script.js" =>
      ZIO.access[Blocking](_.get.blockingExecutor.asEC)
        .flatMap { bc =>
          val blocker = Blocker.liftExecutionContext(bc)
          StaticFile
            .fromResource[LoggingTask]("js/script.js", blocker, None)
            .getOrElseF(NotFound())
        }

    case GET -> Root / slug =>
      Queries.selectUrl(slug).map { uri =>
        Response()
          .withStatus(Status.Found)
          .withHeaders(Location(Uri.unsafeFromString(uri)))
      }
  }.orNotFound

  def genSlug(id: Long): String = {
    val sb = new StringBuilder(8)

    def longToChar(i: Long): Char = {
      if (i < 26) ('a' + i).toChar
      else if (i < 52) ('A' + i - 26).toChar
      else ('0' + i - 52).toChar
    }

    def loop(i: Long, nth: Int): Unit =
      if (nth == 0) ()
      else if (i <= 0) loop(Long.MaxValue-id-4, nth)
      else {
        val next = Math.floorDiv(i, 62)
        val mod  = Math.floorMod(i, 62)
        sb.append(longToChar(mod))
        loop(next, nth-1)
      }

    loop(id, 8)
    sb.result()
  }

}
