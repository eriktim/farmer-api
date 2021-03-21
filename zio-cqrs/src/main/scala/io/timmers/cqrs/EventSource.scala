package io.timmers.cqrs

trait EventSource[E] {
  def streamType: String

  def serialize(event: E): (String, String) // TODO Chunk[Byte]

  def deserialize(eventType: String, eventPayload: String): Either[String, E]
}

object EventSource {
  def apply[E](implicit eventSource: EventSource[E]): EventSource[E] = eventSource

  implicit class EventSourceOps[E](private val event: E) extends AnyVal {
    def streamType(implicit eventSource: EventSource[E]): String =
      eventSource.streamType

    def serialize(implicit eventSource: EventSource[E]): (String, String) =
      eventSource.serialize(event)
  }
}
