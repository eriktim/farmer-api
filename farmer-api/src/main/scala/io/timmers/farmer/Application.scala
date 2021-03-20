package io.timmers.farmer

import zio._
import zhttp.http._
import zhttp.service.Server

object Application extends App {
  val app = Http.collect[Request] {
    case Method.GET -> Root / "text" => Response.text("Hello World!")
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    system.envOrElse("PORT", "8080")
      .map(_.toInt)
      .flatMap(port => Server.start(port, app))
      .exitCode
}