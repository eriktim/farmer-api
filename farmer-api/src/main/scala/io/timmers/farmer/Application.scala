package io.timmers.farmer

import io.timmers.cqrs.EventStore
import io.timmers.farmer.aggregate.Farmer
import io.timmers.farmer.aggregate.Farmer.FarmerCommand.CreateFarmer
import io.timmers.farmer.aggregate.Farmer.FarmerEvent
import zhttp.http.HttpContent._
import zhttp.http._
import zhttp.service.Server

import zio._

object Application extends App {
  val app: Http[ZEnv with EventStore.EventStore[FarmerEvent], HttpError, Request, Response] =
    Http.collectM[Request] { case req @ Method.POST -> Root / "createFarmer" =>
      req.data.content match {
        case Empty => ZIO.fail(HttpError.BadRequest("missing payload"))
        case Complete(data) =>
          Farmer.aggregate
            .sendCommand(CreateFarmer(data))
            .bimap[HttpError, Response](err => HttpError.BadRequest(err), _ => Response.ok)
        case Chunked(_) => ZIO.fail(HttpError.NotImplemented("chunked"))
      }
    }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    system
      .envOrElse("PORT", "8080")
      .map(_.toInt)
      .flatMap(port => Server.start(port, app))
      .provideCustomLayer(EventStore.inMemory[FarmerEvent])
      .exitCode
}
