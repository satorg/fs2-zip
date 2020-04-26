name := "fs2-zip"
organization := "satorg"
version := "0.1-SNAPSHOT"

scalaVersion := "2.13.2"
scalacOptions ++= Seq(
  "-encoding", "utf-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-explaintypes",
  "-language:higherKinds",
  "-Xfatal-warnings",
  "-Ywarn-dead-code",
)

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % Versions.fs2,
  "co.fs2" %% "fs2-io" % Versions.fs2,
  "org.specs2" %% "specs2-core" % Versions.specs2 % Test,
  "org.specs2" %% "specs2-scalacheck" % Versions.specs2 % Test,
)
