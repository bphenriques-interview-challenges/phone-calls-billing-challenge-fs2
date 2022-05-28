package com.bphenriques.billing.logic

import cats.effect.{IO, Ref}
import cats.syntax.all._
import com.bphenriques.billing.model.{Bill, CallRecord}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.concurrent.TimeUnit
import scala.collection.immutable.TreeMap
import scala.concurrent.duration.{Duration, DurationInt}

trait Tariff {
  def singleCall(call: CallRecord): IO[Bill]
  def multipleCalls(calls: fs2.Stream[IO, CallRecord]): IO[Bill]
}

object Tariff {

  implicit private val log: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val PriceUpTo5Minutes = 0.05
  val PriceAfter5Minutes = 0.02

  // Single calls up to 5 minutes cost 0.05 cents per minute.
  // Single calls from 5 minutes onwards cost 0.02 cents per minute.
  // The caller with the most calls will not pay.
  // If there are multiple callers with the same duration, drop both of them.
  def default: Tariff = new Tariff {
    val PriceRange: TreeMap[Duration, BigDecimal] = TreeMap(
      5.minutes - 1.second -> PriceUpTo5Minutes,
      5.minutes -> PriceAfter5Minutes
    )

    def priceAt(duration: Duration): BigDecimal = PriceRange(
      PriceRange
        .rangeFrom(duration)
        .headOption
        .map { case (duration, _) => duration }
        .getOrElse(PriceRange.rangeTo(duration).lastKey)
    )

    override def singleCall(call: CallRecord): IO[Bill] =
      if (call.duration == Duration.Zero) {
        Bill.Empty.pure[IO]
      } else {
        IO(
          Bill(
            call.duration,
            (0L to call.duration.toMinutes)
              .map(minutes => priceAt(Duration(minutes, TimeUnit.MINUTES)))
              .sum
          )
        )
      }

    def createBillPerCaller(records: fs2.Stream[IO, CallRecord]): IO[Map[String, Bill]] =
      fs2.Stream
        .eval(Ref.of[IO, Map[String, Bill]](Map.empty))
        .flatMap { callerToBill =>
          records
            .groupAdjacentBy(_.from)
            .parEvalMap(16) { case (caller, records) =>
              records
                .traverse(singleCall)
                .map(_.combineAll(Bill.Monoid))
                .flatMap { bill =>
                  callerToBill.update { map =>
                    val currentBill = map.getOrElse(caller, Bill.Empty)
                    map + (caller -> Bill.Monoid.combine(currentBill, bill))
                  }
                }
            }
            .evalMap(_ => callerToBill.get)
        }
        .compile
        .last
        .map(_.getOrElse(Map.empty))

    override def multipleCalls(calls: fs2.Stream[IO, CallRecord]): IO[Bill] =
      createBillPerCaller(calls)
        .flatMap { billPerCaller =>
          val callersHighestTotalDuration = billPerCaller.values.map(_.callsDuration).maxOption match {
            case Some(total) => billPerCaller.filter { case (_, bill) => bill.callsDuration == total }
            case None        => billPerCaller
          }

          val result = billPerCaller
            .removedAll(callersHighestTotalDuration.map { case (caller, _) => caller })
            .values
            .toList
            .combineAll(Bill.Monoid)

          log.debug(s"All bills per caller: $billPerCaller") >>
            log.debug(s"Callers with highest total duration: $callersHighestTotalDuration") >>
            log.debug(s"Final bill: $result").as(result)
        }
  }
}
