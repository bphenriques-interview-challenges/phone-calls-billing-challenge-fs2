package com.bphenriques.billing.logic

import cats.effect.IO
import com.bphenriques.billing.model.Bill
import fs2.io.file.{Files, Path}
import fs2.text
import munit.CatsEffectSuite

class BillingTest extends CatsEffectSuite {

  private val validSamplesFolder: Path = Path(getClass.getClassLoader.getResource("valid-samples").getPath)
  private val invalidSamplesFolder: Path = Path(getClass.getClassLoader.getResource("invalid-samples").getPath)

  test("Invalid CSV files") {
    val billing = Billing()
    Files[IO]
      .list(invalidSamplesFolder, ".csv")
      .evalMap { inputFile =>
        billing
          .process(inputFile)
          .map(_.format(Bill.DefaultFormat))
          .intercept[Throwable]
      }
  }

  test("Valid CSV files") {
    val billing = Billing()
    Files[IO]
      .list(validSamplesFolder, ".csv")
      .evalMap { inputFile =>
        val expectedFile = inputFile.parent.get / s"${inputFile.fileName}.expected"
        Files[IO]
          .readAll(expectedFile)
          .through(text.utf8.decode)
          .compile
          .last
          .map(_.get)
          .flatMap { expected =>
            billing
              .process(inputFile)
              .map(_.format(Bill.DefaultFormat))
              .assertEquals(expected)
          }
      }
  }
}
