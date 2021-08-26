name := "fs2-zip"
organization := "satorg"
organizationName := "Sergey Torgashov"

homepage := Some(url("https://github.com/satorg/fs2-zip"))
licenses := List(("MIT", url("http://opensource.org/licenses/MIT")))
startYear := Some(2020)

scalaVersion := Versions.Scala2_13
crossScalaVersions := Seq(Versions.Scala2_12, Versions.Scala2_13)

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
  "co.fs2" %% "fs2-core" % Versions.Fs2,
  "co.fs2" %% "fs2-io" % Versions.Fs2,
  "org.scalameta" %% "munit" % Versions.MUnit % Test,
  "org.scalameta" %% "munit-scalacheck" % Versions.MUnit % Test,
  "org.typelevel" %% "munit-cats-effect-2" % Versions.MUnitCatsEffect % Test,
  "org.typelevel" %% "scalacheck-effect-munit" % Versions.ScalacheckEffect % Test
)
