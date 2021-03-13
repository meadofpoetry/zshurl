import Dependencies._

ThisBuild / scalaVersion     := "2.13.4"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "org.meadofpoetry"
ThisBuild / organizationName := "meadofpoetry"

lazy val root = (project in file("."))
  .settings(projectSettings)

val projectSettings = Seq(
  name := "zshurl",
  libraryDependencies ++= Seq(
    // Zio
    "dev.zio" %% "zio" % "1.0.5",
    "dev.zio" %% "zio-interop-cats" % "2.3.1.0",
    "dev.zio" %% "zio-logging" % "0.5.3",

    // Http4s
    "org.http4s" %% "http4s-dsl" % http4sVer,
    "org.http4s" %% "http4s-blaze-server" % http4sVer,
    "org.http4s" %% "http4s-circe" % http4sVer,

    // Circe
    "io.circe" %% "circe-core" % circeVer,
    "io.circe" %% "circe-generic" % circeVer,
    "io.circe" %% "circe-parser" % circeVer,
    //"io.circe" %% "circe-generic-extras" % circeVer,

    // Doobie
    "org.tpolecat" %% "doobie-core"      % doobieVer,
    "org.tpolecat" %% "doobie-postgres"  % doobieVer,
    "org.tpolecat" %% "doobie-hikari"    % doobieVer,
    //"org.tpolecat" %% "doobie-specs2"    % doobieVer % "test",
    "org.tpolecat" %% "doobie-scalatest" % doobieVer % "test",

    //"com.github.pureconfig" %% "pureconfig" % "0.14.1"

    scalaTest % Test
  )
)

val scala2_13 = "2.13.3"
val scala2_12 = "2.12.8"

val http4sVer = "0.21.20"
val circeVer  = "0.13.0"
val doobieVer = "0.12.1"
