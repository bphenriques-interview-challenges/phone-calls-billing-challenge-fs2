package com.bphenriques.billing.model

import cats.Monoid

import java.text.DecimalFormat
import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class Bill(maxDuration: FiniteDuration, totalDuration: FiniteDuration, cost: BigDecimal) {
  def format(format: DecimalFormat): String = format.format(cost)
}

object Bill {
  val Empty: Bill = Bill(0.seconds, 0.seconds, 0)
  val DefaultFormat: DecimalFormat = new DecimalFormat("0.00")

  val Monoid: Monoid[Bill] = new Monoid[Bill] {
    override def empty: Bill = Bill.Empty
    override def combine(x: Bill, y: Bill): Bill = Bill(
      x.maxDuration.max(y.maxDuration),
      x.totalDuration + y.totalDuration,
      x.cost + y.cost
    )
  }
}
