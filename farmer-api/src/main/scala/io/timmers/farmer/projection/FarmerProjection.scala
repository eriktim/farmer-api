package io.timmers.farmer.projection

import io.timmers.cqrs.Projection
import io.timmers.farmer.aggregate.Farmer.FarmerEvent
import zio.ZIO

object FarmerProjection {
  val projection = Projection.create[FarmerEvent, Seq[Farmer]](Seq(), _ => ZIO.unit)

  case class Farmer(name: String)
}
