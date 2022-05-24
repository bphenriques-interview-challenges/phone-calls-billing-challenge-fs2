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
        log.debug("Creating the set of bill for each caller...") >>
          recordsToBillPerCaller(records)
            .map { billsPerCaller =>
              val highestDuration = billsPerCaller.toList
                .sortBy { case (_, bill) => bill }(Bill.TotalDurationOrdering.reverse)
                .map { case (_, highestDuration) => highestDuration.maxDuration }
                .headOption

              val callersSharingHighDuration = highestDuration match {
                case Some(duration) => billsPerCaller.takeWhile { case (_, bill) => bill.maxDuration == duration }
                case None           => List.empty
              }

              billsPerCaller
                .removedAll(callersSharingHighDuration.map { case (caller, _) => caller })
                .values
                .toList
                .combineAll(Bill.Monoid)
            }
    }
  }
}
