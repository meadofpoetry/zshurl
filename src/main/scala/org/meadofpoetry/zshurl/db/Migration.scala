package org.meadofpoetry.zshurl.db

import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._
import doobie._
import doobie.implicits._

object Migration {


  def up: RIO[DB, Unit] =
    ZIO.accessM[DB]{ conn =>
      val q = for {
        _ <- createLinks.run
        _ <- createVisits.run
      } yield (())
      q.transact(conn.get)
    }

  def down: RIO[DB, Unit] =
    ZIO.accessM[DB]{ conn =>
      val q = for {
        _ <- dropLinks.run
        _ <- dropVisits.run
      } yield (())
      q.transact(conn.get)
    }

  private lazy val createLinks =
    sql"""
     CREATE TABLE links(
       id BIGSERIAL NOT NULL PRIMARY KEY,
       slug VARCHAR(8) NOT NULL UNIQUE,
       url TEXT,
       visits BIGINT DEFAULT NULL,
       created TIMESTAMP WITH TIME ZONE DEFAULT NULL,
       last_visited TIMESTAMP WITH TIME ZONE DEFAULT NULL
     )""".update

  private lazy val createVisits =
    sql"""
CREATE TABLE visits(
  id BIGSERIAL NOT NULL PRIMARY KEY,
  slug VARCHAR(8) DEFAULT NULL,
  visit_date TIMESTAMP WITH TIME ZONE DEFAULT NULL,
  referer TEXT
)""".update

  private lazy val dropLinks =
    sql"DROP TABLE IF EXISTS links".update

  private lazy val dropVisits =
    sql"DROP TABLE IF EXISTS visits".update

}
