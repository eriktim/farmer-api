val zioVersion     = "1.0.4-2"
val zioHttpVersion = "1.0.0.0-RC13"

scalaVersion := "2.13.3"

enablePlugins(JavaAppPackaging)

addCommandAlias("fix", "scalafixAll")
addCommandAlias("fixCheck", "scalafixAll --check")
addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll")
addCommandAlias("prepare", "fix; fmt")

lazy val root =
  project
    .in(file("."))
    .settings(mainClass in Compile := Some("io.timmers.farmer.Application"))
    .aggregate(farmerApi, zioCqrs)
    .dependsOn(farmerApi, zioCqrs)

lazy val farmerApi =
  module("farmer-api", "farmer-api")
    .settings(
      libraryDependencies ++= Seq(
        "io.d11" %% "zhttp" % zioHttpVersion
      )
    )
    .dependsOn(zioCqrs)

lazy val zioCqrs = module("cqrs", "zio-cqrs")

def module(moduleName: String, fileName: String): Project =
  Project(moduleName, file(fileName))
    .settings(
      scalaVersion := "2.13.3",
      organization := "io.timmers",
      name := moduleName,
      version := "0.1.0",
      scalacOptions := Seq(
        "-Ywarn-unused:_",
        "-Xfatal-warnings"
      ),
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio"         % zioVersion,
        "dev.zio" %% "zio-streams" % zioVersion
      ),
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio-test"     % zioVersion % Test,
        "dev.zio" %% "zio-test-sbt" % zioVersion % Test
      ),
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      semanticdbEnabled := true,
      semanticdbVersion := scalafixSemanticdb.revision,
      ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(
        scalaVersion.value
      ),
      ThisBuild / scalafixDependencies ++= List(
        "com.github.liancheng" %% "organize-imports" % "0.4.0",
        "com.github.vovapolu"  %% "scaluzzi"         % "0.1.12"
      )
    )
