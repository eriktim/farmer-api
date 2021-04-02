package io.timmers.cqrs

import io.timmers.cqrs.Event.Payload
import io.timmers.cqrs.EventStream.EventStream
import zio.stream.Sink
import zio.{ Tag, ZIO }

trait Projection[R, E, Q, S] {
  def read(query: Q): ZIO[R, E, S]
}

object Projection {
  def create[P <: Payload: Tag](): Projection[EventStream[P], String, String, String] =
    new Projection[EventStream[P], String, String, String] {
      override def read(query: String): ZIO[EventStream[P], String, String] =
        EventStream.subscribe[P]().run(Sink.foldLeft("TODO")((agg, _) => agg))
    }
}
