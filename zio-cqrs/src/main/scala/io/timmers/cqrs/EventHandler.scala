package io.timmers.cqrs

import zio.ZIO

trait EventHandler[E <: Event.Payload] {
  def handleEvent(event: E): ZIO[EventStore.EventStore[E], String, Unit]
}
