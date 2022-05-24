package com.bphenriques.billing.logic

import cats.effect.IO
import com.bphenriques.billing.model.Bill
import fs2.io.file.Path

import scala.util.chaining.scalaUtilChainingOps

trait Billing {
  def process(path: Path): IO[Bill]
}

object Billing {
  def apply(): Billing = new Billing {
    override def process(path: Path): IO[Bill] = {
      val processor = Tariff.multipleMultiple(Tariff.singleRecord)
      CallRecordsProvider
        .fromCSV(path)
        .read()
        .pipe(processor.process)
    }
  }
}
