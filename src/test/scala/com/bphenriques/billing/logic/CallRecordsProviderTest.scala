package com.bphenriques.billing.logic

import com.bphenriques.billing.model.{CallRecord, Contact}
import fs2.io.file.Path
import munit.CatsEffectSuite

import java.time.LocalTime

class CallRecordsProviderTest extends CatsEffectSuite {

  private val ValidSamplesFolder: Path = Path(getClass.getClassLoader.getResource("valid-samples").getPath)

  test("It parses the files") {
    CallRecordsProvider
      .fromCSV(ValidSamplesFolder / "after-midnight.csv")
      .read()
      .compile
      .toList
      .assertEquals(
        List(
          CallRecord(LocalTime.parse("23:58:00"), LocalTime.parse("00:01:59"), Contact("A"), Contact("B")),
          CallRecord(LocalTime.parse("23:50:00"), LocalTime.parse("00:10:00"), Contact("B"), Contact("A"))
        )
      )
  }
}
