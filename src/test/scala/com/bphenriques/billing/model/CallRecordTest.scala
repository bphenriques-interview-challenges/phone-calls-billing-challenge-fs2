package com.bphenriques.billing.model

import munit.FunSuite

import java.time.LocalTime
import scala.concurrent.duration.DurationInt

class CallRecordTest extends FunSuite {

  test("Contact Eq") {
    val a = Contact("A")
    val b = Contact("B")

    assert(Contact.Eq.eqv(a, a))
    assert(Contact.Eq.neqv(a, b))
    assert(Contact.Eq.neqv(b, a))
  }

  test("Call Record apply with start before end") {
    val duration = 10.seconds
    val record = CallRecord(LocalTime.NOON, LocalTime.NOON.plusSeconds(duration.toSeconds), Contact("A"), Contact("B"))
    assertEquals(record.from, Contact("A"))
    assertEquals(record.to, Contact("B"))
    assertEquals(record.duration, duration)
  }

  test("Call Record apply with end before start") {
    val duration = 5.seconds
    val record = CallRecord(
      LocalTime.MIDNIGHT.minusSeconds(duration.toSeconds),
      LocalTime.MIDNIGHT.plusSeconds(duration.toSeconds),
      Contact("A"),
      Contact("B")
    )
    assertEquals(record.from, Contact("A"))
    assertEquals(record.to, Contact("B"))
    assertEquals(record.duration, duration * 2)
  }
}
