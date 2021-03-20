val zioVersion = "1.0.4-2"
val zioHttpVersion = "1.0.0.0-RC13"

scalaVersion := "2.13.3"

enablePlugins(JavaAppPackaging)

lazy val root =
  project
    .in(file("."))
    .aggregate(farmerApi, zioCqrs)

lazy val zioCqrs = module("cqrs", "zio-cqrs")

lazy val farmerApi = module("farmer-api", "farmer-api")
  .settings(
    libraryDependencies ++= Seq(
      "io.d11" %% "zhttp" % zioHttpVersion
    )
  )
  .dependsOn(zioCqrs)

def module(moduleName: String, fileName: String): Project =
  Project(moduleName, file(fileName))
    .settings(
      scalaVersion := "2.13.1",
      organization := "io.timmers",
      name := moduleName,
      version := "0.1.0",
      scalacOptions := Seq(
        "-Ywarn-unused:_",
        "-Xfatal-warnings"
      ),
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % zioVersion
      ),
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio-test"     % zioVersion % Test,
        "dev.zio" %% "zio-test-sbt" % zioVersion % Test
      ),
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
    )
