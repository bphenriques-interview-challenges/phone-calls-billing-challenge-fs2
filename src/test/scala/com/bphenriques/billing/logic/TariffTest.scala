package com.bphenriques.billing.logic

import com.bphenriques.billing.logic.Tariff.{PriceAfter5Minutes, PriceUpTo5Minutes}
import com.bphenriques.billing.model.{Bill, CallRecord}
import munit.CatsEffectSuite

import java.time.LocalTime
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

class TariffTest extends CatsEffectSuite {

  private def callWithDuration(duration: FiniteDuration): CallRecord =
    CallRecord(LocalTime.NOON, LocalTime.NOON.plus(duration.toNanos, ChronoUnit.NANOS), "A", "B")

  test("Zero Duration Call Record") {
    val duration = Duration.Zero
    Tariff.default.singleCall(callWithDuration(duration)).assertEquals(Bill.Empty)
  }

  test("1 Second Call Record") {
    val duration = 1.second
    Tariff.default
      .singleCall(callWithDuration(duration))
      .assertEquals(Bill(duration, 1 * PriceUpTo5Minutes))
  }

  test("Right before first minute") {
    val duration = 59.second
    Tariff.default
      .singleCall(callWithDuration(duration))
      .assertEquals(Bill(duration, 1 * PriceUpTo5Minutes))
  }

  test("Right at 1 minute") {
    val duration = 60.seconds
    Tariff.default
      .singleCall(callWithDuration(duration))
      .assertEquals(Bill(duration, 2 * PriceUpTo5Minutes))
  }

  test("Right before five minutes") {
    val duration = 5.minutes - 1.seconds
    Tariff.default
      .singleCall(callWithDuration(duration))
      .assertEquals(Bill(duration, 5 * PriceUpTo5Minutes))
  }

  test("Right at five minutes") {
    val duration = 5.minutes
    Tariff.default
      .singleCall(callWithDuration(duration))
      .assertEquals(Bill(duration, 5 * PriceUpTo5Minutes + 1 * PriceAfter5Minutes))
  }

  test("10 minutes call") {
    val duration = 10.minutes
    Tariff.default
      .singleCall(callWithDuration(duration))
      .assertEquals(Bill(duration, 5 * PriceUpTo5Minutes + 6 * PriceAfter5Minutes))
  }

  test("Multiple records with only 1 caller returns a bill with no cost") {
    Tariff.default
      .multipleCalls(
        fs2.Stream(
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON, "A", "B"),
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON, "A", "B")
        )
      )
      .assertEquals(Bill.Empty)
  }

  test("Multiple records with 2 caller returns a bill with the caller with less phone-calls") {
    Tariff.default
      .multipleCalls(
        fs2.Stream(
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON.minusSeconds(1), "A", "B"),
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON, "B", "A")
        )
      )
      .assertEquals(Bill(12.hours - 1.second, BigDecimal(14.55)))
  }

  test("Multiple records with 2 caller returns a empty bill if both share the same total duration") {
    Tariff.default
      .multipleCalls(
        fs2.Stream(
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON, "A", "B"),
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON, "B", "A")
        )
      )
      .assertEquals(Bill.Empty)
  }

  test("Multiple records with 3 caller returns the bill of one as the other two share the same total duration") {
    Tariff.default
      .multipleCalls(
        fs2.Stream(
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON.minusSeconds(1), "C", "A"),
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON, "A", "B"),
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON, "B", "A")
        )
      )
      .assertEquals(Bill(12.hours - 1.second, BigDecimal(14.55)))
  }
}
