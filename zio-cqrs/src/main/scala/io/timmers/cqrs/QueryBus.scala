package io.timmers.cqrs

import io.timmers.cqrs.Event.Payload
import zio.ZIO

trait QueryBus[R, E, Q, S] {
  def read(query: Q): ZIO[R, E, S]
}

object QueryBus {
  def fromProjection[P <: Payload](): QueryBus[EventStream[P], String, String, String] = ???
}