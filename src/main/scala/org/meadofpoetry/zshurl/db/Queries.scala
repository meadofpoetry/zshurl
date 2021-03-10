package org.meadofpoetry.zshurl.db

import zio._
import zio.clock._
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.logging._
import doobie._
import doobie.implicits._
import doobie.implicits.javatime._
import java.time.OffsetDateTime

object Queries {

  private def insertLinkQuery(url: String, slug: String, created: OffsetDateTime) =
    sql"INSERT INTO links(url, slug, visits, created) VALUES ($url, $slug, ${0}, $created)".update

  private val selectCurrentLinksIdQuery =
    sql"SELECT last_value FROM links_id_seq".query[Long].unique

  private def selectUrlQuery(slug: String) =
    sql"SELECT url FROM links WHERE slug = $slug".query[String].unique

  private def updateLinkQuery(slug: String, lastVisited: OffsetDateTime) =
    sql"UPDATE links SET visits = visits + 1, last_visited = $lastVisited WHERE slug = $slug".update

  private def updateVisitsQuery(slug: String, lastVisited: OffsetDateTime, referer: String) =
    sql"INSERT INTO visits(slug, visit_date, referer) VALUES ($slug, $lastVisited, $referer)".update

  def insertLink(url: String, slug: String, created: OffsetDateTime): RIO[DB, Unit] =
    ZIO.accessM[DB](conn =>
      insertLinkQuery(url, slug, created).run.transact(conn.get) *> ZIO.succeed(())
    )

  def selectCurrentLinksId: RIO[DB, Long] =
    ZIO.accessM[DB](conn =>
      selectCurrentLinksIdQuery.transact(conn.get)
    )

  def selectUrl(slug: String): RIO[DB, String] =
    ZIO.accessM[DB](conn =>
      selectUrlQuery(slug).transact(conn.get)
    )

  def updateOnVisit(
    slug: String,
    lastVisited: OffsetDateTime,
    referer: String
  ) = ZIO.accessM[DB] { conn =>
    val q = for {
      _ <- updateLinkQuery(slug, lastVisited).run
      _ <- updateVisitsQuery(slug, lastVisited, referer).run
    } yield (())
    q.transact(conn.get)
  }
}
