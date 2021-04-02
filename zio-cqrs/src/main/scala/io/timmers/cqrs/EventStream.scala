package io.timmers.cqrs

import zio.stream.ZStream
import zio.{ Has, Tag }

object EventStream {
  type EventStream[P <: Event.Payload] = Has[EventStream.Service[P]]

  trait Service[P <: Event.Payload] {
    def subscribe(): ZStream[Any, String, Event[P]]
  }

  def subscribe[P <: Event.Payload: Tag](): ZStream[EventStream[P], String, Event[P]] =
    ZStream.accessStream(_.get.subscribe())

}
