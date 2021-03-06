name := "fs2-zip"
organization := "satorg"
organizationName := "Sergey Torgashov"

homepage := Some(url("https://github.com/satorg/fs2-zip"))
licenses := List(("MIT", url("http://opensource.org/licenses/MIT")))
startYear := Some(2020)

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
  "org.specs2" %% "specs2-scalacheck" % Versions.specs2 % Test,
  "com.codecommit" %% "cats-effect-testing-specs2" % Versions.catsEffectTesting % Test
)
