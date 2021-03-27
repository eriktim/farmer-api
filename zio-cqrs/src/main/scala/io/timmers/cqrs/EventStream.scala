package io.timmers.cqrs

import zio.stream.ZStream

trait EventStream[P <: Event.Payload] {
  def subscribe(): ZStream[Any, String, Event[P]]
}
