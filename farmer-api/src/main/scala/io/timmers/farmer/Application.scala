package io.timmers.farmer

import io.timmers.cqrs._
import io.timmers.farmer.aggregate.FarmerAggregate
import io.timmers.farmer.aggregate.FarmerAggregate.FarmerCommand.CreateFarmer
import io.timmers.farmer.aggregate.FarmerAggregate.FarmerEvent
import io.timmers.farmer.projection.FarmerProjection
import io.timmers.farmer.projection.FarmerProjection.Farmer
import zhttp.http.HttpContent._
import zhttp.http._
import zhttp.service.Server
import zio._

object Application extends App {
  def http(
    projection: Projection[Any, String, String, Seq[Farmer]]
  ): Http[ZEnv with EventStore.EventStore[FarmerEvent], HttpError, Request, Response] =
    Http.collectM[Request] {
      case req @ Method.POST -> Root / "createFarmer" =>
        req.data.content match {
          case Empty => ZIO.fail(HttpError.BadRequest("missing payload"))
          case Complete(data) =>
            FarmerAggregate.aggregate
              .sendCommand(CreateFarmer(data))
              .bimap[HttpError, Response](HttpError.BadRequest, _ => Response.ok)
          case Chunked(_) => ZIO.fail(HttpError.NotImplemented("chunked"))
        }
      case Method.GET -> Root / "getFarmers" =>
        projection
          .read("TEST")
          .bimap(HttpError.BadRequest, farmers => Response.jsonString(farmers.toString()))
    }

  def app(
    storageRef: Ref[Seq[Event[FarmerEvent]]],
    queue: Queue[Event[FarmerEvent]],
    stateRef: Ref[Seq[Farmer]]
  ): ZIO[zio.ZEnv, Nothing, ExitCode] =
    (for {
      port       <- system.envOrElse("PORT", "8080")
      projection <- FarmerProjection.projection(stateRef)
      server     <- Server.start(port.toInt, http(projection))
    } yield server)
      .provideCustomLayer(EventStore.inMemory(storageRef, queue) ++ EventStream.inMemory(queue))
      .exitCode

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] = for {
    storageRef <- Ref.make(Seq[Event[FarmerEvent]]())
    queue      <- Queue.unbounded[Event[FarmerEvent]]
    stateRef   <- Ref.make(Seq[Farmer]())
    exitCode   <- app(storageRef, queue, stateRef)
  } yield exitCode
}
