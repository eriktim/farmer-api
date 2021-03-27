package io.timmers.cqrs

import io.timmers.cqrs.Aggregate.AggregateEnv
import zio.test.Assertion.{ equalTo, hasSize, nothing }
import zio.test.{ Assertion, TestResult, assert }
import zio.{ Ref, Tag, UIO, ZEnv, ZIO, ZLayer }

package object test {
  class AggregateTester[C <: Command, S, E <: Event.Payload: Tag](
    val aggregate: Aggregate[AggregateEnv[E], C, S],
    val commands: Seq[C]
  ) {
    def whenCommand(command: C): AggregateVerifier[S, E] = {
      val effect = Ref.make((Seq[Event[E]](), Seq[E]())).flatMap { storage =>
        val eventStore = new InMemoryEventStore[E](storage)
        val result = for {
          _          <- ZIO.foreach_(commands)(aggregate.sendCommand)
          state      <- aggregate.sendCommand(command)
          lastEvents <- eventStore.lastEvents
        } yield (state, lastEvents)
        val layer: ZLayer[ZEnv, Nothing, EventStore.EventStore[E]] = ZLayer.succeed(eventStore)
        result.provideCustomLayer(layer)
      }
      new AggregateVerifier[S, E](effect)
    }
  }

  class AggregateVerifier[S, E](val effect: ZIO[ZEnv, String, (S, Seq[E])]) {
    def thenEvents(assertions: Assertion[E]*): ZIO[ZEnv, Nothing, TestResult] =
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

  implicit class AggregateTest[C <: Command, S, E <: Event.Payload: Tag](
    val aggregate: Aggregate[AggregateEnv[E], C, S]
  ) {
    def givenNoCommands(): AggregateTester[C, S, E] =
      new AggregateTester[C, S, E](aggregate, Seq())

    def givenCommands(commands: C*): AggregateTester[C, S, E] =
      new AggregateTester[C, S, E](aggregate, commands)
  }

  class InMemoryEventStore[E <: Event.Payload: Tag](storageRef: Ref[(Seq[Event[E]], Seq[E])])
      extends EventStore.Service[E] {

    override def readEvents(aggregateId: String): ZIO[Any, String, Seq[Event[E]]] =
      storageRef.get.map(_._1.filter(event => event.payload.aggregateId == aggregateId))

    override def persistEvents(events: Seq[Event[E]]): ZIO[Any, String, Unit] =
      storageRef.update(storage => (storage._1 ++ events, events.map(_.payload)))

    def lastEvents: UIO[Seq[E]] =
      storageRef.get.map(_._2)
  }
}
