package io.timmers.cqrs

trait Event[+E <: Event.Payload] {
  def sequenceNumber: Long
  def timestamp: Long
  def payload: E
}

object Event {
  trait Payload {
    def aggregateId: String
  }
}
