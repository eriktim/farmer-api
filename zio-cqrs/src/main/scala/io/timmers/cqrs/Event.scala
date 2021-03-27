package io.timmers.cqrs

trait Event[+P <: Event.Payload] {
  def sequenceNumber: Long
  def timestamp: Long
  def payload: P
}

object Event {
  trait Payload {
    def aggregateId: String
  }
}
