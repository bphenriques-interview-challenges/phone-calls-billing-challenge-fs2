package com.bphenriques.billing.logic

import cats.effect.IO
import com.bphenriques.billing.model.{CallRecord, Decoders}
import fs2.data.csv.{CsvRowDecoder, decodeGivenHeaders}
import fs2.io.file.{Files, Path}
import fs2.text

trait CallRecordsProvider {
  def read(): fs2.Stream[IO, CallRecord]
}

object CallRecordsProvider {
  def fromCSV(path: Path): CallRecordsProvider = new CallRecordsProvider {
    override def read(): fs2.Stream[IO, CallRecord] = {
      implicit val decoder: CsvRowDecoder[CallRecord, String] = Decoders.CallRecordWithHeaders

      Files[IO]
        .readAll(path)
        .through(text.utf8.decode)
        .through(decodeGivenHeaders[CallRecord](Decoders.CsvColumns.All, skipHeaders = false, separator = ';'))
    }
  }
}
