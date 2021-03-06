package com.bphenriques.billing.model

import munit.FunSuite

import scala.concurrent.duration.{Duration, DurationInt}

class BillTest extends FunSuite {

  test("Empty Bill") {
    assertEquals(Bill.Empty, Bill(Duration.Zero, BigDecimal(0)))
  }

  // op(x, zero) == x and op(zero, x) == x
  test("Monoid - Identity") {
    val x = Bill(2.seconds, BigDecimal(1))
    val zero = Bill.Empty

    val monoid = Bill.Monoid
    assertEquals(monoid.combine(x, zero), monoid.combine(zero, x))
  }

  // op(op(x,y), z) == op(x, op(y,z))
  test("Monoid - Associativity") {
    val x = Bill(2.seconds, BigDecimal(1))
    val y = Bill(3.seconds, BigDecimal(4))
    val z = Bill(4.seconds, BigDecimal(5))

    val monoid = Bill.Monoid
    assertEquals(monoid.combine(monoid.combine(x, y), z), monoid.combine(x, monoid.combine(y, z)))
  }

  test("Format") {
    assertEquals(
      Bill(2.seconds, BigDecimal(1)).format(Bill.DefaultFormat),
      "1.00"
    )
  }
}
