package org.meadofpoetry.zshurl.api

import org.http4s._
import org.http4s.headers.Location
import org.http4s.implicits._
import org.http4s.util.{ CaseInsensitiveString => CIString }
import zio._
import zio.clock._
import zio.logging._
import zio.interop.catz._
import org.meadofpoetry.zshurl.db._
import org.meadofpoetry.zshurl.util._
import org.meadofpoetry.zshurl.api.Http

object Core {

  import zdsl._

  val service = HttpRoutes.of[ApiTask] {

    case request @ GET -> Root =>
      Http.staticResource("index.html", request, NotFound())

    case request @ GET -> Root / "js" / "script.js" =>
      Http.staticResource("js/script.js", request, NotFound())

    case request @ POST -> Root / "api" / "shortify" =>
      for {
        state <- ZIO.access[State](_.get)
        url   <- Http.parse[UrlMessage](request)
        _     <- log.info("shortifying url: " + url.url)
        id    <- state.id.updateAndGet(_ + 1)
        slug  =  genURLSlug(id)
        time  <- currentDateTime
        resp  <- Queries.insertLink(url.url.toString(), slug, time).foldCauseM(
          err => Http.error(s"failed to insert link $url with slug $slug"),
          _   => Http.ok(UrlMessage(state.uriTemplate.withPath(slug)))
        )
      } yield resp

    case request @ GET -> Root / "api" / "original" =>
      for {
        state     <- ZIO.access[State](_.get)
        req       <- Http.parse[UrlMessage](request)
        slug      = req.url.path.tail
        longURL   <- Queries.selectUrl(slug)
        time      <- currentDateTime
        userAgent = request.headers.find(
          _.name == CIString("User-Agent")
        )
        _         <- Queries.updateOnVisit(
          slug, time, userAgent.map(_.value).getOrElse("")
        )
        resp      <- Uri.fromString(longURL).fold(
          err => Http.error("failed to retrieve URL"),
          ok  => Http.ok(UrlMessage(ok))
        )
      } yield resp

    case request @ GET -> Root / slug =>
      for {
        longURL   <- Queries.selectUrl(slug)
        time      <- currentDateTime
        userAgent = request.headers.find(
          _.name == CIString("User-Agent")
        )
        _         <- Queries.updateOnVisit(
          slug, time, userAgent.map(_.value).getOrElse("")
        )
      } yield Response()
        .withStatus(Status.Found)
        .withHeaders(Location(Uri.unsafeFromString(longURL)))

  }.orNotFound

}
