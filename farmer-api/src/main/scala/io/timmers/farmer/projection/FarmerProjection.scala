package io.timmers.farmer.projection

import zio.UIO

object FarmerProjection {
  case class Farmer(name: String)

  val farmers = UIO(List(Farmer("Henk")))
}
