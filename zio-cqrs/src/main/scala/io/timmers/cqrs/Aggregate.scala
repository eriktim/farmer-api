package io.timmers.cqrs

import zio.clock.{ Clock, currentTime }
import zio.{ Tag, ZIO }

import java.util.concurrent.TimeUnit

trait Aggregate[R, C <: Command, S] {
  def sendCommand(command: C): ZIO[R, String, S]

  def getState(aggregateId: String): ZIO[R, String, S]
}

trait Command {
  def aggregateId: String
}

object Aggregate {
  type AggregateEnv[E <: Event.Payload]                    = EventStore.EventStore[E] with Clock
  type CommandHandler[C <: Command, S, E <: Event.Payload] = (C, S) => ZIO[Any, String, Seq[E]]
  type EventHandler[S, E <: Event.Payload]                 = (Event[E], S) => S

  case class AggregateEvent[E <: Event.Payload](sequenceNumber: Long, timestamp: Long, payload: E)
      extends Event[E]

  def create[C <: Command, S, E <: Event.Payload: Tag](
    initialState: S,
    commandHandler: CommandHandler[C, S, E],
    eventHandler: EventHandler[S, E]
  ): Aggregate[AggregateEnv[E], C, S] =
    new Aggregate[AggregateEnv[E], C, S] {
      override def sendCommand(command: C): ZIO[AggregateEnv[E], String, S] =
        for {
          state    <- readState(command.aggregateId)
          payloads <- commandHandler(command, state._2)
          events   <- createEvents(payloads, state._1 + 1)
          _        <- EventStore.persistEvents(events)
          state    <- buildState(state._2, events)
        } yield state

      override def getState(aggregateId: String): ZIO[AggregateEnv[E], String, S] =
        readState(aggregateId).map(_._2)

      private def readState(aggregateId: String): ZIO[AggregateEnv[E], String, (Long, S)] =
        for {
          events            <- EventStore.readEvents[E](aggregateId)
          lastSequenceNumber = if (events.isEmpty) 0L else events.map(_.sequenceNumber).max
          state             <- buildState(initialState, events)
        } yield (lastSequenceNumber, state)

      private def buildState(initialState: S, events: Seq[Event[E]]): ZIO[Any, String, S] =
        ZIO.foldLeft(events)(initialState)((state, event) =>
          ZIO.effect(eventHandler(event, state)).mapError(_.getMessage)
        )

      private def createEvents(
        payloads: Seq[E],
        nextSequenceNumber: Long
      ): ZIO[Clock, Nothing, Seq[Event[E]]] =
        ZIO
          .foldLeft(payloads)((nextSequenceNumber, Seq[Event[E]]())) {
            case ((sequenceNumber, events), payload) =>
              createEvent(payload, sequenceNumber).map(event =>
                (sequenceNumber + 1, events :+ event)
              )
          }
          .map(_._2)

      private def createEvent(payload: E, sequenceNumber: Long): ZIO[Clock, Nothing, Event[E]] =
        currentTime(TimeUnit.SECONDS).map(AggregateEvent(sequenceNumber, _, payload))
    }
}
