package io.timmers.cqrs

import io.timmers.cqrs.CommandBus.AggregateEnv
import io.timmers.cqrs.test.AggregateTest
import zio.ZIO
import zio.test.Assertion.{ anything, dies, equalTo }
import zio.test._

object CommandBusTesterSpec extends DefaultRunnableSpec {
  private val aggregate: CommandBus[AggregateEnv[TestEvent], TestCommand, String, Option[Int]] =
    CommandBus.fromAggregate(None, handleCommand, handleEvent)

  val aggregateId: String = "AggregateId"

  def spec: ZSpec[Environment, Failure] =
    suite("AggregateTester Spec")(
      testM("should return events for valid commands") {
        aggregate
          .givenNoCommands()
          .whenCommand(AcceptCommand(aggregateId))
          .thenEvents(equalTo(AcceptedEvent(aggregateId)))
      },
      testM("should return an error for invalid commands") {
        aggregate
          .givenNoCommands()
          .whenCommand(DeclineCommand(aggregateId))
          .thenError(equalTo("invalid command"))
      },
      testM("should set state for valid commands") {
        aggregate
          .givenNoCommands()
          .whenCommand(AcceptCommand(aggregateId))
          .thenState(equalTo(Some(1)))
      },
      testM("should update state for valid commands") {
        aggregate
          .givenCommands(AcceptCommand(aggregateId))
          .whenCommand(AcceptCommand(aggregateId))
          .thenState(equalTo(Some(2)))
      },
      testM("should build state for commands of the same aggregate") {
        aggregate
          .givenCommands(AcceptCommand("AnotherAggregateId"))
          .whenCommand(AcceptCommand(aggregateId))
          .thenState(equalTo(Some(1)))
      },
      testM("should die on command exceptions") {
        val effect = aggregate
          .givenNoCommands()
          .whenCommand(ThrowCommand(aggregateId))
          .thenEvents()
        assertM(effect.run)(dies(anything))
      },
      testM("should block aggregate instance on event exceptions") {
        aggregate
          .givenNoCommands()
          .whenCommand(CorruptCommand(aggregateId))
          .thenError(equalTo("aggregate is corrupted"))
      }
    )

  sealed trait TestCommand                       extends Command
  case class AcceptCommand(aggregateId: String)  extends TestCommand
  case class DeclineCommand(aggregateId: String) extends TestCommand
  case class ThrowCommand(aggregateId: String)   extends TestCommand
  case class CorruptCommand(aggregateId: String) extends TestCommand

  sealed trait TestEvent                         extends Event.Payload
  case class AcceptedEvent(aggregateId: String)  extends TestEvent
  case class CorruptedEvent(aggregateId: String) extends TestEvent

  private def handleCommand(
    command: TestCommand,
    state: Option[Int]
  ): ZIO[Any, String, Seq[TestEvent]] = command match {
    case AcceptCommand(aggregateId)  => ZIO.succeed(Seq(AcceptedEvent(aggregateId)))
    case DeclineCommand(_)           => ZIO.fail("invalid command")
    case ThrowCommand(_)             => ZIO.succeed(throw new RuntimeException(state.toString))
    case CorruptCommand(aggregateId) => ZIO.succeed(Seq(CorruptedEvent(aggregateId)))
  }

  private def handleEvent(event: Event[TestEvent], state: Option[Int]): Option[Int] =
    event.payload match {
      case AcceptedEvent(_)  => state.map(_ + 1).orElse(Some(1))
      case CorruptedEvent(_) => throw new RuntimeException("aggregate is corrupted")
    }
}
