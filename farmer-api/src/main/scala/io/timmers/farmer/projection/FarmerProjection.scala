package io.timmers.farmer.projection

import io.timmers.cqrs.EventStream.EventStream
import io.timmers.cqrs.Projection
import io.timmers.farmer.aggregate.FarmerAggregate.FarmerEvent
import io.timmers.farmer.aggregate.FarmerAggregate.FarmerEvent.FarmerCreated
import zio.{ Ref, ZIO }

object FarmerProjection {
  def projection(
    stateRef: Ref[Seq[Farmer]]
  ): ZIO[EventStream[FarmerEvent], String, Projection[Any, String, String, Seq[Farmer]]] =
    Projection.create[FarmerEvent, Seq[Farmer]](
      stateRef,
      (state, event) =>
        event.payload match {
          case FarmerCreated(aggregateId) => {
            state.update(s => s :+ Farmer(aggregateId))
          }
        }
    )

  case class Farmer(name: String)
}
