package io.timmers.cqrs

import io.timmers.cqrs.Event.Payload
import io.timmers.cqrs.EventStream.EventStream
import zio.stream.Sink
import zio.{ Ref, Tag, ZIO }

trait Projection[R, E, Q, S] {
  def read(query: Q): ZIO[R, E, S]
}

object Projection {
  type EventHandler[E, P <: Event.Payload, S] = (Ref[S], Event[P]) => ZIO[Any, E, Any]

  def create[P <: Payload: Tag, S](
    state: Ref[S],
    eventHandler: EventHandler[String, P, S]
  ): ZIO[EventStream[P], String, Projection[Any, String, String, S]] = for {
    _ <- EventStream
           .subscribe[P]()
           .run(Sink.foreach(event => eventHandler.apply(state, event)))
           .fork
  } yield new Projection[Any, String, String, S] {
    override def read(query: String): ZIO[Any, String, S] =
      state.get

  }
}
