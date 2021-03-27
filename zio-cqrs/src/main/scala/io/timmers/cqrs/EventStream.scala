package io.timmers.cqrs

import zio.stream.ZStream

trait EventStream[E <: Event.Payload] {
  def subscribe(): ZStream[Any, String, E]
}
