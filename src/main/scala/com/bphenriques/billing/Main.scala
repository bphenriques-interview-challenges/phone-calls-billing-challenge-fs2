package com.bphenriques.billing

import cats.syntax.all._
import cats.effect.{ExitCode, IO, IOApp}
import com.bphenriques.billing.logic.Billing
import com.bphenriques.billing.model.Bill
import fs2.io.file.Path
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp {

  implicit private val log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    if (args.length != 1) {
      IO.println("Usage: <file>").as(ExitCode.Error)
    } else {
      val path = Path(args.head)
      val billing = Billing()
      billing
        .process(path)
        .flatMap(bill => IO.println(bill.format(Bill.DefaultFormat)))
        .as(ExitCode.Success)
        .recoverWith(t => log.error(t)(s"Error processing file $path.").as(ExitCode.Error))
    }
}
