package io.timmers.cqrs

import io.timmers.cqrs.CommandBus.AggregateEnv
import zio.test.Assertion.{ equalTo, hasSize, nothing }
import zio.test.{ Assertion, TestResult, assert }
import zio.{ Ref, Tag, UIO, ZEnv, ZIO, ZLayer }

package object test {
  class AggregateTester[C <: Command, S, P <: Event.Payload: Tag](
    val aggregate: CommandBus[AggregateEnv[P], C, String, S],
    val commands: Seq[C]
  ) {
    def whenCommand(command: C): AggregateVerifier[S, P] = {
      val effect = Ref.make((Seq[Event[P]](), Seq[P]())).flatMap { storage =>
        val eventStore = new InMemoryEventStore[P](storage)
        val result = for {
          _          <- ZIO.foreach_(commands)(aggregate.sendCommand)
          state      <- aggregate.sendCommand(command)
          lastEvents <- eventStore.lastEvents
        } yield (state, lastEvents)
        val layer: ZLayer[ZEnv, Nothing, EventStore.EventStore[P]] = ZLayer.succeed(eventStore)
        result.provideCustomLayer(layer)
      }
      new AggregateVerifier[S, P](effect)
    }
  }

  class AggregateVerifier[S, P](val effect: ZIO[ZEnv, String, (S, Seq[P])]) {
    def thenEvents(assertions: Assertion[P]*): ZIO[ZEnv, Nothing, TestResult] =
      effect.fold(
        error => assert(error)(nothing),
        state =>
          if (state._2.length != assertions.length)
            assert(state._2)(hasSize(equalTo(assertions.length)))
          else
            state._2
              .zip(assertions)
              .map { case (event, assertion) => assert(event)(assertion) }
              .reduce(_ && _)
      )

    def thenState(assertion: Assertion[S]): ZIO[ZEnv, Nothing, TestResult] =
      effect.fold(
        error => assert(error)(nothing),
        state => assert(state._1)(assertion)
      )

    def thenError(assertion: Assertion[String]): ZIO[ZEnv, Nothing, TestResult] =
      effect.fold(
        error => assert(error)(assertion),
        _ => assert("\"no error\"")(assertion)
      )
  }

  implicit class AggregateTest[C <: Command, S, P <: Event.Payload: Tag](
    val aggregate: CommandBus[AggregateEnv[P], C, String, S]
  ) {
    def givenNoCommands(): AggregateTester[C, S, P] =
      new AggregateTester[C, S, P](aggregate, Seq())

    def givenCommands(commands: C*): AggregateTester[C, S, P] =
      new AggregateTester[C, S, P](aggregate, commands)
  }

  class InMemoryEventStore[P <: Event.Payload: Tag](storageRef: Ref[(Seq[Event[P]], Seq[P])])
      extends EventStore.Service[P] {

    override def readEvents(aggregateId: String): ZIO[Any, String, Seq[Event[P]]] =
      storageRef.get.map(_._1.filter(event => event.payload.aggregateId == aggregateId))

    override def persistEvents(events: Seq[Event[P]]): ZIO[Any, String, Unit] =
      storageRef.update(storage => (storage._1 ++ events, events.map(_.payload)))

    def lastEvents: UIO[Seq[P]] =
      storageRef.get.map(_._2)
  }
}
