package io.timmers.cqrs

import io.timmers.cqrs.Event.Payload
import io.timmers.cqrs.EventStream.EventStream
import zio.ZIO

trait QueryBus[R, E, Q, S] {
  def read(query: Q): ZIO[R, E, S]
}

object QueryBus {
  def fromProjection[P <: Payload](): QueryBus[EventStream[P], String, String, String] =
    new QueryBus[EventStream[P], String, String, String] {
      override def read(query: String): ZIO[EventStream[P], String, String] =
        for {
          result <- EventStream.subscribe[P]().fold("TODO")((s, _) => s)
        } yield result
    }
}
