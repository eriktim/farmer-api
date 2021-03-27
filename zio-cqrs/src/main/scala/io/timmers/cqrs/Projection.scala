package io.timmers.cqrs

import io.timmers.cqrs.Event.Payload
import zio.ZIO

trait Projection[R, Q, S] {
  def read(query: Q): ZIO[Any, String, S]
}

object Projection {
  def create[E <: Payload](): Projection[EventStream[E], String, String] = ???
}
