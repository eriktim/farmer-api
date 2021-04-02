package io.timmers.cqrs

import zio.clock.{ Clock, currentTime }
import zio.{ Tag, ZIO }

import java.util.concurrent.TimeUnit

trait CommandBus[R, C <: Command, E, S] {
  def sendCommand(command: C): ZIO[R, E, S]

  def getState(aggregateId: String): ZIO[R, E, S]
}

trait Command {
  def aggregateId: String
}

object CommandBus {
  type AggregateEnv[P <: Event.Payload]                    = EventStore.EventStore[P] with Clock
  type CommandHandler[C <: Command, S, E <: Event.Payload] = (C, S) => ZIO[Any, String, Seq[E]]
  type EventHandler[S, E <: Event.Payload]                 = (Event[E], S) => S

  case class AggregateEvent[E <: Event.Payload](sequenceNumber: Long, timestamp: Long, payload: E)
      extends Event[E]

  def create[C <: Command, S, P <: Event.Payload: Tag](
    initialState: S,
    commandHandler: CommandHandler[C, S, P],
    eventHandler: EventHandler[S, P]
  ): CommandBus[AggregateEnv[P], C, String, S] =
    new CommandBus[AggregateEnv[P], C, String, S] {
      override def sendCommand(command: C): ZIO[AggregateEnv[P], String, S] =
        for {
          state    <- readState(command.aggregateId)
          payloads <- commandHandler(command, state._2)
          events   <- createEvents(payloads, state._1 + 1)
          _        <- EventStore.persistEvents(events)
          state    <- buildState(state._2, events)
        } yield state

      override def getState(aggregateId: String): ZIO[AggregateEnv[P], String, S] =
        readState(aggregateId).map(_._2)

      private def readState(aggregateId: String): ZIO[AggregateEnv[P], String, (Long, S)] =
        for {
          events            <- EventStore.readEvents[P](aggregateId)
          lastSequenceNumber = if (events.isEmpty) 0L else events.map(_.sequenceNumber).max
          state             <- buildState(initialState, events)
        } yield (lastSequenceNumber, state)

      private def buildState(initialState: S, events: Seq[Event[P]]): ZIO[Any, String, S] =
        ZIO.foldLeft(events)(initialState)((state, event) =>
          ZIO.effect(eventHandler(event, state)).mapError(_.getMessage)
        )

      private def createEvents(
        payloads: Seq[P],
        nextSequenceNumber: Long
      ): ZIO[Clock, Nothing, Seq[Event[P]]] =
        ZIO
          .foldLeft(payloads)((nextSequenceNumber, Seq[Event[P]]())) {
            case ((sequenceNumber, events), payload) =>
              createEvent(payload, sequenceNumber).map(event =>
                (sequenceNumber + 1, events :+ event)
              )
          }
          .map(_._2)

      private def createEvent(payload: P, sequenceNumber: Long): ZIO[Clock, Nothing, Event[P]] =
        currentTime(TimeUnit.SECONDS).map(AggregateEvent(sequenceNumber, _, payload))
    }
}
