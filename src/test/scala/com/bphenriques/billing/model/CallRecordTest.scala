package com.bphenriques.billing.model

import munit.FunSuite

import java.time.LocalTime
import scala.concurrent.duration.DurationInt

class CallRecordTest extends FunSuite {

  test("Call Record apply with start before end") {
    val duration = 10.seconds
    val record = CallRecord(LocalTime.NOON, LocalTime.NOON.plusSeconds(duration.toSeconds), "A", "B")
    assertEquals(record.from, "A")
    assertEquals(record.to, "B")
    assertEquals(record.duration, duration)
  }

  test("Call Record apply with end before start") {
    val duration = 5.seconds
    val record = CallRecord(
      LocalTime.MIDNIGHT.minusSeconds(duration.toSeconds),
      LocalTime.MIDNIGHT.plusSeconds(duration.toSeconds),
      "A",
      "B"
    )
    assertEquals(record.from, "A")
    assertEquals(record.to, "B")
    assertEquals(record.duration, duration * 2)
  }
}
