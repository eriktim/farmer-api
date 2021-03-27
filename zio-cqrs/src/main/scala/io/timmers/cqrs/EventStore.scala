package io.timmers.cqrs

import zio.{ Has, Tag, UIO, ZIO, ZLayer }

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

  def inMemory[P <: Event.Payload: Tag]: ZLayer[Any, Nothing, EventStore[P]] =
    ZLayer.succeed(
      new Service[P] {
        private var storage = Seq[Event[P]]()

        override def readEvents(aggregateId: String): ZIO[Any, String, Seq[Event[P]]] =
          UIO(storage.filter(event => event.payload.aggregateId == aggregateId))

        override def persistEvents(events: Seq[Event[P]]): ZIO[Any, String, Unit] =
          UIO(storage ++= events)
      }
    )
}
