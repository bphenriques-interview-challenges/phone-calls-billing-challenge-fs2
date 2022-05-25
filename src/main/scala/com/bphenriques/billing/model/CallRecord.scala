package com.bphenriques.billing.model

import java.time.{LocalDate, LocalDateTime, LocalTime, Duration => JDuration}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

case class CallRecord private (duration: FiniteDuration, from: String, to: String)

object CallRecord {

  private val ReferenceDate = LocalDate.of(2000, 1, 1)

  def apply(startTime: LocalTime, endTime: LocalTime, from: String, to: String): CallRecord = {
    val (start, end) = if (endTime.isBefore(startTime)) {
      (
        LocalDateTime.of(ReferenceDate, startTime),
        LocalDateTime.of(ReferenceDate.plusDays(1), endTime)
      )
    } else {
      (
        LocalDateTime.of(ReferenceDate, startTime),
        LocalDateTime.of(ReferenceDate, endTime)
      )
    }

    val duration: FiniteDuration = FiniteDuration(JDuration.between(start, end).toSeconds, TimeUnit.SECONDS)
    new CallRecord(duration, from, to)
  }
}
