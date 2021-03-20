package io.timmers.cqrs

import zio.{ Has, Tag, UIO, ZIO, ZLayer }

object EventStore {
  type EventStore[E <: Event.Payload] = Has[EventStore.Service[E]]

  trait Service[E <: Event.Payload] {
    def readEvents(aggregateId: String): ZIO[Any, String, Seq[Event[E]]]

    def persistEvents(events: Seq[Event[E]]): ZIO[Any, String, Unit]
  }

  def readEvents[E <: Event.Payload: Tag](
    aggregateId: String
  ): ZIO[EventStore[E], String, Seq[Event[E]]] =
    ZIO.accessM(_.get.readEvents(aggregateId))

  def persistEvents[E <: Event.Payload: Tag](
    events: Seq[Event[E]]
  ): ZIO[EventStore[E], String, Unit] =
    ZIO.accessM(_.get.persistEvents(events))

  def inMemory[E <: Event.Payload: Tag]: ZLayer[Any, Nothing, EventStore[E]] =
    ZLayer.succeed(
      new Service[E] {
        private var storage = Seq[Event[E]]()

        override def readEvents(aggregateId: String): ZIO[Any, String, Seq[Event[E]]] =
          UIO(storage.filter(event => event.payload.aggregateId == aggregateId))

        override def persistEvents(events: Seq[Event[E]]): ZIO[Any, String, Unit] =
          UIO(storage ++= events)
      }
    )
}
