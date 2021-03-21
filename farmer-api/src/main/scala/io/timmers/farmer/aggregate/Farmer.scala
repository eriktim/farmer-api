package io.timmers.farmer.aggregate

import io.timmers.cqrs.{ Aggregate, Command, Event }
import io.timmers.farmer.aggregate.Farmer.FarmerCommand.CreateFarmer
import io.timmers.farmer.aggregate.Farmer.FarmerEvent.FarmerCreated

import zio.ZIO

object Farmer {
  val aggregate: Aggregate[FarmerCommand, Option[String], FarmerEvent] =
    Aggregate.create(None, handleCommand, handleEvent)

  sealed trait FarmerCommand extends Command

  object FarmerCommand {
    case class CreateFarmer(aggregateId: String) extends FarmerCommand
  }

  sealed trait FarmerEvent extends Event.Payload

  object FarmerEvent {
    case class FarmerCreated(aggregateId: String) extends FarmerEvent
  }

  private def handleCommand(
    command: FarmerCommand,
    state: Option[String]
  ): ZIO[Any, String, Seq[FarmerEvent]] = command match {
    case CreateFarmer(aggregateId) =>
      state match {
        case None    => ZIO.succeed(Seq(FarmerCreated(aggregateId)))
        case Some(_) => ZIO.fail("farmer already exists")
      }
  }

  private def handleEvent(event: Event[FarmerEvent], state: Option[String]): Option[String] =
    event.payload match {
      case FarmerCreated(aggregateId) => Some(aggregateId)
      case _                          => state
    }
}
