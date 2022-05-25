package com.bphenriques.billing.logic

import cats.effect.IO
import com.bphenriques.billing.model.Bill
import fs2.io.file.Path
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.util.chaining.scalaUtilChainingOps

trait Billing {
  def process(path: Path): IO[Bill]
}

object Billing {

  implicit private val log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def apply(): Billing = new Billing {
    override def process(path: Path): IO[Bill] = {
      val processor = Tariff.multipleMultiple(Tariff.singleRecord)

      log.debug(s"Computing the bill given CSV file at '$path'...") >>
        CallRecordsProvider
          .fromCSV(path)
          .read()
          .pipe(processor.process)
    }
  }
}
