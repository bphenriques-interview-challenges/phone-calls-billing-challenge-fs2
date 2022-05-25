package com.bphenriques.billing.logic

import com.bphenriques.billing.logic.Tariff.{PriceAfter5Minutes, PriceUpTo5Minutes}
import com.bphenriques.billing.model.{Bill, CallRecord, Contact}
import munit.CatsEffectSuite

import java.time.LocalTime
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

class TariffTest extends CatsEffectSuite {

  private def callWithDuration(duration: FiniteDuration): CallRecord =
    CallRecord(LocalTime.NOON, LocalTime.NOON.plus(duration.toNanos, ChronoUnit.NANOS), Contact("A"), Contact("B"))

  test("Zero Duration Call Record") {
    val duration = Duration.Zero
    Tariff.singleRecord.process(callWithDuration(duration)).assertEquals(Bill.Empty)
  }

  test("1 Second Call Record") {
    val duration = 1.second
    Tariff.singleRecord
      .process(callWithDuration(duration))
      .assertEquals(Bill(duration, 1 * PriceUpTo5Minutes))
  }

  test("Right before first minute") {
    val duration = 59.second
    Tariff.singleRecord
      .process(callWithDuration(duration))
      .assertEquals(Bill(duration, 1 * PriceUpTo5Minutes))
  }

  test("Right at 1 minute") {
    val duration = 60.seconds
    Tariff.singleRecord
      .process(callWithDuration(duration))
      .assertEquals(Bill(duration, 2 * PriceUpTo5Minutes))
  }

  test("Right before five minutes") {
    val duration = 5.minutes - 1.seconds
    Tariff.singleRecord
      .process(callWithDuration(duration))
      .assertEquals(Bill(duration, 5 * PriceUpTo5Minutes))
  }

  test("Right at five minutes") {
    val duration = 5.minutes
    Tariff.singleRecord
      .process(callWithDuration(duration))
      .assertEquals(Bill(duration, 5 * PriceUpTo5Minutes + 1 * PriceAfter5Minutes))
  }

  test("10 minutes call") {
    val duration = 10.minutes
    Tariff.singleRecord
      .process(callWithDuration(duration))
      .assertEquals(Bill(duration, 5 * PriceUpTo5Minutes + 6 * PriceAfter5Minutes))
  }

  test("Multiple records with only 1 caller returns a bill with no cost") {
    Tariff
      .multipleMultiple(Tariff.singleRecord)
      .process(
        fs2.Stream(
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON, Contact("A"), Contact("B")),
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON, Contact("A"), Contact("B"))
        )
      )
      .assertEquals(Bill.Empty)
  }

  test("Multiple records with 2 caller returns a bill with the caller with less phone-calls") {
    Tariff
      .multipleMultiple(Tariff.singleRecord)
      .process(
        fs2.Stream(
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON.minusSeconds(1), Contact("A"), Contact("B")),
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON, Contact("B"), Contact("A"))
        )
      )
      .assertEquals(Bill(12.hours - 1.second, BigDecimal(14.55)))
  }

  test("Multiple records with 2 caller returns a empty bill if both share the same total duration") {
    Tariff
      .multipleMultiple(Tariff.singleRecord)
      .process(
        fs2.Stream(
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON, Contact("A"), Contact("B")),
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON, Contact("B"), Contact("A"))
        )
      )
      .assertEquals(Bill.Empty)
  }

  test("Multiple records with 3 caller returns the bill of one as the other two share the same total duration") {
    Tariff
      .multipleMultiple(Tariff.singleRecord)
      .process(
        fs2.Stream(
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON.minusSeconds(1), Contact("C"), Contact("A")),
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON, Contact("A"), Contact("B")),
          CallRecord(LocalTime.MIDNIGHT, LocalTime.NOON, Contact("B"), Contact("A"))
        )
      )
      .assertEquals(Bill(12.hours - 1.second, BigDecimal(14.55)))
  }
}
