name := "fs2-zip"
ThisBuild / organization := "satorg"
ThisBuild / organizationName := "Sergey Torgashov"

ThisBuild / homepage := Some(url("https://github.com/satorg/fs2-zip"))
ThisBuild / licenses := List(("MIT", url("http://opensource.org/licenses/MIT")))
ThisBuild / startYear := Some(2020)

ThisBuild / crossScalaVersions := Seq(Versions.Scala2_12, Versions.Scala2_13)
ThisBuild / scalaVersion := Versions.Scala2_13
ThisBuild / scalacOptions ++= Seq(
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

ThisBuild / libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % Versions.Fs2,
  "co.fs2" %% "fs2-io" % Versions.Fs2,
  "org.scalameta" %% "munit" % Versions.MUnit % Test,
  "org.scalameta" %% "munit-scalacheck" % Versions.MUnit % Test,
  "org.typelevel" %% "munit-cats-effect-2" % Versions.MUnitCatsEffect % Test,
  "org.typelevel" %% "scalacheck-effect-munit" % Versions.ScalacheckEffect % Test
)

ThisBuild / githubWorkflowPublishTargetBranches := Nil
ThisBuild / githubWorkflowBuildPreamble +=
  WorkflowStep.Sbt("preprCheck" :: Nil, name = Some("Check that code is formatted"))

inThisBuild(Nil ++
  addCommandAlias("prepr", "headerCreateAll; scalafmtAll; scalafmtSbt") ++
  addCommandAlias("preprCheck", "headerCheckAll; scalafmtCheckAll; scalafmtSbtCheck"))
