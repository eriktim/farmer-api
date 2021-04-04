package io.timmers.cqrs

import zio.stream.ZStream
import zio.{ Has, Queue, Tag, ZLayer }

object EventStream {
  type EventStream[P <: Event.Payload] = Has[EventStream.Service[P]]

  trait Service[P <: Event.Payload] {
    def subscribe(): ZStream[Any, String, Event[P]]
  }

  def inMemory[P <: Event.Payload: Tag](
    queue: Queue[Event[P]]
  ): ZLayer[Any, Nothing, EventStream[P]] =
    ZLayer.succeed(new InMemory[P](queue))

  def subscribe[P <: Event.Payload: Tag](): ZStream[EventStream[P], String, Event[P]] =
    ZStream.accessStream(_.get.subscribe())

  class InMemory[P <: Event.Payload: Tag](
    queue: Queue[Event[P]]
  ) extends EventStream.Service[P] {

    override def subscribe(): ZStream[Any, String, Event[P]] =
      ZStream.fromQueue(queue)
  }
}
