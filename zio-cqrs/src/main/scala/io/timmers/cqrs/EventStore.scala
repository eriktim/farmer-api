package io.timmers.cqrs

import zio.{ Has, Queue, Ref, Tag, ZIO, ZLayer }

object EventStore {
  type EventStore[P <: Event.Payload] = Has[EventStore.Service[P]]

  trait Service[P <: Event.Payload] {
    def readEvents(aggregateId: String): ZIO[Any, String, Seq[Event[P]]]

    def persistEvents(events: Seq[Event[P]]): ZIO[Any, String, Unit]
  }

  def readEvents[P <: Event.Payload: Tag](
    aggregateId: String
  ): ZIO[EventStore[P], String, Seq[Event[P]]] =
    ZIO.accessM(_.get.readEvents(aggregateId))

  def persistEvents[P <: Event.Payload: Tag](
    events: Seq[Event[P]]
  ): ZIO[EventStore[P], String, Unit] =
    ZIO.accessM(_.get.persistEvents(events))

  def inMemory[P <: Event.Payload: Tag](
    storageRef: Ref[Seq[Event[P]]],
    queue: Queue[Event[P]]
  ): ZLayer[Any, Nothing, EventStore[P]] =
    ZLayer.succeed(new InMemory[P](storageRef, queue))

  class InMemory[P <: Event.Payload: Tag](
    storageRef: Ref[Seq[Event[P]]],
    queue: Queue[Event[P]]
  ) extends EventStore.Service[P] {

    override def readEvents(aggregateId: String): ZIO[Any, String, Seq[Event[P]]] =
      storageRef.get.map(_.filter(event => event.payload.aggregateId == aggregateId))

    override def persistEvents(events: Seq[Event[P]]): ZIO[Any, String, Unit] = for {
      _ <- storageRef.update(storage => storage ++ events)
      _ <- queue.offerAll(events)
    } yield ()
  }
}
