package com.bphenriques.billing.model

import cats.data.NonEmptyList
import fs2.data.csv._

import java.time.LocalTime
import java.time.format.DateTimeFormatter

object Decoders {

  object CsvColumns {
    val TimeOfStart = "time_of_start"
    val TimeOfFinish = "time_of_finish"
    val CallFrom = "call_from"
    val CallTo = "call_to"

    val All: NonEmptyList[String] = NonEmptyList.of(TimeOfStart, TimeOfFinish, CallFrom, CallTo)
  }

  object CallRecordWithHeaders extends CsvRowDecoder[CallRecord, String] {
    private val TimestampFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    implicit val timeDecoder: CellDecoder[LocalTime] = CellDecoder.localTimeDecoder(TimestampFormat)
    implicit val contactDecoder: CellDecoder[Contact] =
      CellDecoder[String].emap(s =>
        Either.cond(!s.isBlank, Contact(s.trim), new DecoderError(s"The contact must be a non-blank string"))
      )

    def apply(row: CsvRow[String]): DecoderResult[CallRecord] =
      for {
        startTime <- row.as[LocalTime](CsvColumns.TimeOfStart)
        endTime <- row.as[LocalTime](CsvColumns.TimeOfFinish)
        from <- row.as[Contact](CsvColumns.CallFrom)
        to <- row.as[Contact](CsvColumns.CallTo)
      } yield CallRecord(startTime, endTime, from, to)
  }
}
