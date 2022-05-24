package com.bphenriques.billing.model

import munit.FunSuite

import scala.concurrent.duration.{Duration, DurationInt}

class BillTest extends FunSuite {

  test("Empty Bill") {
    assertEquals(Bill.Empty, Bill(Duration.Zero, Duration.Zero, BigDecimal(0)))
  }

  // op(x, zero) == x and op(zero, x) == x
  test("Monoid - Identity") {
    val x = Bill(1.second, 2.seconds, BigDecimal(1))
    val zero = Bill.Empty

    val monoid = Bill.Monoid
    assertEquals(monoid.combine(x, zero), monoid.combine(zero, x))
  }

  // op(op(x,y), z) == op(x, op(y,z))
  test("Monoid - Associativity") {
    val x = Bill(1.second, 2.seconds, BigDecimal(1))
    val y = Bill(2.seconds, 3.seconds, BigDecimal(4))
    val z = Bill(4.seconds, 4.seconds, BigDecimal(5))

    val monoid = Bill.Monoid
    assertEquals(monoid.combine(monoid.combine(x, y), z), monoid.combine(x, monoid.combine(y, z)))
  }

  test("TotalDurationOrdering") {
    val x = Bill(1.second, 2.seconds, BigDecimal(1))
    val y = Bill(2.seconds, 3.seconds, BigDecimal(4))
    val z = Bill(4.seconds, 4.seconds, BigDecimal(5))

    val ordering = Bill.TotalDurationOrdering

    assert(ordering.compare(x, x) == 0)
    assert(ordering.compare(x, y) == -1)
    assert(ordering.compare(y, z) == -1)
    assert(ordering.compare(z, x) == 1)
  }

  test("Format") {
    assertEquals(
      Bill(1.second, 2.seconds, BigDecimal(1)).format(Bill.DefaultFormat),
      "1.00"
    )
  }

  test("toString") {
    assertEquals(
      Bill(1.second, 2.seconds, BigDecimal(1)).toString,
      "1.00"
    )
  }
}
