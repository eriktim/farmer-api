package io.timmers.farmer.projection

import io.timmers.cqrs.EventStream.EventStream
import io.timmers.cqrs.Projection
import io.timmers.farmer.aggregate.FarmerAggregate.FarmerEvent
import zio.ZIO

object FarmerProjection {
  val projection
    : ZIO[EventStream[FarmerEvent], String, Projection[Any, String, String, Seq[Farmer]]] =
    Projection.create[FarmerEvent, Seq[Farmer]](Seq(), _ => ZIO.unit)

  case class Farmer(name: String)
}
