package org.meadofpoetry.zshurl.api

import org.http4s.{ HttpRoutes, Response, Status, Uri }
import org.http4s.headers.Location
import org.http4s.dsl.Http4sDsl
import org.http4s.server.staticcontent._
import org.http4s.implicits._
import zio._
import zio.clock._
import zio.interop.catz._
import zio.logging._
import org.meadofpoetry.zshurl.db._
import org.meadofpoetry.zshurl.util._

object Core {

  import zdsl._

  val service = HttpRoutes.of[ApiTask] {

    case request @ GET -> Root =>
      staticResource("index.html", request, NotFound())

    case request @ GET -> Root / "js" / "script.js" =>
      staticResource("js/script.js", request, NotFound())

    case request @ POST -> Root / "api" / "shortify" =>
      for {
        state <- ZIO.access[State](_.get)
        url64 <- request.bodyAsText.compile.string
        url   =  base64decode(url64)
        _     <- log.debug("shortifying url: " + url)
        id    <- state.id.updateAndGet(_ + 1)
        slug  =  genURLSlug(id)
        time  <- currentDateTime
        _     <- Queries.insertLink(url, slug, time).foldCauseM(
          err => log.error(s"failed to insert link $url with slug $slug"),
          _   => ZIO.succeed(())
        )
        _     <- log.info("server: " + request.serverAddr + ":" + request.serverPort)
        short = state.uriTemplate.withPath(slug)
        res   <- Ok(base64encode(short.toString()))
      } yield res

    case request @ GET -> Root / slug =>
      for {
        longURL   <- Queries.selectUrl(slug)
        time      <- currentDateTime
        userAgent = request.headers.find(
          _.name == org.typelevel.ci.CIString("User-Agent")
        )
        _         <- Queries.updateOnVisit(
          slug, time, userAgent.map(_.value).getOrElse("")
        )
      } yield Response()
        .withStatus(Status.Found)
        .withHeaders(Location(Uri.unsafeFromString(longURL)))

  }.orNotFound

}
