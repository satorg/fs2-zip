name := "fs2-zip"
organization := "satorg"
version := "0.1-SNAPSHOT"

scalaVersion := Versions.`Scala_2.13`
crossScalaVersions := Seq(Versions.`Scala_2.12`, Versions.`Scala_2.13`)

scalacOptions ++= Seq(
  "-encoding",
  "utf-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-explaintypes",
  "-language:higherKinds",
  "-Xlint"
) ++
  CrossVersion.partialVersion(scalaVersion.value).collect {
    case (2, 12) =>
      Seq(
        "-Xfatal-warnings",
        "-Ypartial-unification",
        "-Ywarn-dead-code"
      )
    case (2, minor) if minor >= 13 =>
      Seq(
        "-Wdead-code",
        "-Werror",
        "-Wunused"
      )
  }.toList.flatten

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % Versions.fs2,
  "co.fs2" %% "fs2-io" % Versions.fs2,
  "org.specs2" %% "specs2-core" % Versions.specs2 % Test,
  "org.specs2" %% "specs2-scalacheck" % Versions.specs2 % Test
)
