package io.timmers.cqrs

import io.timmers.cqrs.Event.Payload
import io.timmers.cqrs.EventStream.EventStream
import zio.stream.Sink
import zio.{ Tag, ZIO }

trait Projection[R, E, Q, S] {
  def read(query: Q): ZIO[R, E, S]
}

object Projection {
  type EventHandler[E, P <: Event.Payload] = Event[P] => ZIO[Any, E, Any]

  def create[P <: Payload: Tag, S](
    initialState: S,
    eventHandler: EventHandler[String, P]
  ): ZIO[EventStream[P], String, Projection[Any, String, String, S]] = for {
    _ <- EventStream.subscribe[P]().run(Sink.foreach(eventHandler.apply))
  } yield new Projection[Any, String, String, S] {
    override def read(query: String): ZIO[Any, String, S] =
      ZIO.succeed(initialState)

  }
}
