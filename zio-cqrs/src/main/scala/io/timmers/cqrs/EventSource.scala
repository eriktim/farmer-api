package io.timmers.cqrs

trait EventSource[P] {
  def streamType: String

  def serialize(payload: P): (String, String) // TODO Chunk[Byte]

  def deserialize(eventType: String, eventPayload: String): Either[String, P]
}

object EventSource {
  def apply[P](implicit eventSource: EventSource[P]): EventSource[P] = eventSource

  implicit class EventSourceOps[P](private val payload: P) extends AnyVal {
    def streamType(implicit eventSource: EventSource[P]): String =
      eventSource.streamType

    def serialize(implicit eventSource: EventSource[P]): (String, String) =
      eventSource.serialize(payload)
  }
}
