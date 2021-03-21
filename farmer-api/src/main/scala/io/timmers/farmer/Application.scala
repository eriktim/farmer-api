package io.timmers.farmer

import zhttp.http._
import zhttp.service.Server

import zio._

object Application extends App {
  val app: Http[Any, HttpError, Request, Response] = Http.collect[Request] {
    case Method.GET -> Root / "text" =>
      Response.text("Hello World!")
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    system
      .envOrElse("PORT", "8080")
      .map(_.toInt)
      .flatMap(port => Server.start(port, app))
      .exitCode
}
