package io.timmers.farmer.aggregate

import io.timmers.cqrs.CommandBus.AggregateEnv
import io.timmers.cqrs.{ Command, CommandBus, Event }
import io.timmers.farmer.aggregate.FarmerAggregate.FarmerCommand.CreateFarmer
import io.timmers.farmer.aggregate.FarmerAggregate.FarmerEvent.FarmerCreated
import zio.ZIO

object FarmerAggregate {
  val aggregate: CommandBus[AggregateEnv[FarmerEvent], FarmerCommand, String, Option[String]] =
    CommandBus.create(None, handleCommand, handleEvent)

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
