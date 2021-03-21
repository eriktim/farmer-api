package io.timmers.cqrs

import zio.ZIO

trait Projection[Q, S] {
  def read(query: Q): ZIO[Any, String, S]
}
