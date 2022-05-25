package com.bphenriques.billing.logic

import cats.effect.{IO, Ref}
import cats.syntax.all._
import com.bphenriques.billing.model.{Bill, CallRecord}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.concurrent.TimeUnit
import scala.collection.immutable.TreeMap
import scala.concurrent.duration.{Duration, DurationInt}

trait Tariff[T] {
  def process(value: T): IO[Bill]
}

object Tariff {

  implicit private val log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val PriceUpTo5Minutes = 0.05
  val PriceAfter5Minutes = 0.02

  def singleRecord: Tariff[CallRecord] = new Tariff[CallRecord] {
    val PriceRange: TreeMap[Duration, BigDecimal] = TreeMap(
      5.minutes - 1.second -> PriceUpTo5Minutes,
      5.minutes -> PriceAfter5Minutes
    )

    def priceAt(duration: Duration): BigDecimal = PriceRange(
      PriceRange
        .rangeFrom(duration)
        .headOption
        .map(_._1)
        .getOrElse(PriceRange.rangeTo(duration).lastKey)
    )

    override def process(record: CallRecord): IO[Bill] =
      if (record.duration == Duration.Zero) {
        Bill.Empty.pure[IO]
      } else {
        IO(
          Bill(
            record.duration,
            (0L to record.duration.toMinutes)
              .map(minutes => priceAt(Duration(minutes, TimeUnit.MINUTES)))
              .sum
          )
        )
      }
  }

  // The caller with the most calls will not pay.
  // If there are multiple callers with the same duration, drop both of them.
  def multipleMultiple(singleTariff: Tariff[CallRecord]): Tariff[fs2.Stream[IO, CallRecord]] = {
    def recordsToBillPerCaller(records: fs2.Stream[IO, CallRecord]): IO[Map[String, Bill]] = {
      fs2.Stream
        .eval(Ref.of[IO, Map[String, Bill]](Map.empty))
        .flatMap { callerToBill =>
          records
            .groupAdjacentBy(_.from.phoneNumber)
            .parEvalMap(16) { case (caller, records) =>
              records
                .traverse(singleTariff.process)
                .map(_.combineAll(Bill.Monoid))
                .flatMap { bill =>
                  callerToBill.updateAndGet { map =>
                    map + (caller -> Bill.Monoid.combine(map.getOrElse(caller, Bill.Empty), bill))
                  }
                }
            }
        }
        .compile
        .last
        .map(_.getOrElse(Map.empty))
    }

    new Tariff[fs2.Stream[IO, CallRecord]] {
      override def process(records: fs2.Stream[IO, CallRecord]): IO[Bill] =
        recordsToBillPerCaller(records)
          .flatMap { billsPerCaller =>
            val callersHighestTotalDuration = billsPerCaller.values.map(_.callsDuration).maxOption match {
              case Some(total) => billsPerCaller.filter { case (_, bill) => bill.callsDuration == total }
              case None        => billsPerCaller
            }

            val result = billsPerCaller
              .removedAll(callersHighestTotalDuration.map { case (caller, _) => caller })
              .values
              .toList
              .combineAll(Bill.Monoid)

            log.debug(s"All Bills before discount: $billsPerCaller") >>
              log.debug(s"Callers that spent more time on the phone: $callersHighestTotalDuration") >>
              log.debug(s"Final Bill: $result").as(result)
          }
    }
  }
}
