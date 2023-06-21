package service

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import java.time.LocalDate
import java.time.format.DateTimeParseException

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

import domain.{Convertable, Currency, TradeData}
import external.ExchangeRateService
import cache.ExchangeRateCache
import cache.ExchangeRateCache.CacheResponse

object ConversionService {
  sealed trait Command
  case class ConvertStakeToEUR(convertable: Convertable, replyTo: ActorRef[ConversionResult]) extends Command

  sealed trait ConversionResult
  case class ConvertedStake(amount: Double) extends ConversionResult
  case class ConversionFailure(reason: ConversionError) extends ConversionResult

  sealed trait ConversionError extends Exception
  case class InvalidCurrencyError(currency: String) extends ConversionError {
    override def getMessage: String = s"Invalid currency: $currency"
  }
  case class InvalidDateError(date: String) extends ConversionError {
    override def getMessage: String = s"Invalid date: $date"
  }
  case class ExchangeRateRetrievalError(cause: Throwable) extends ConversionError {
    override def getMessage: String = "Failed to retrieve exchange rate"
    override def getCause: Throwable = cause
  }

  def apply(exchangeRateCache: ActorRef[ExchangeRateCache.Command])(implicit system: ActorSystem[_], executionContext: ExecutionContext): Behavior[Command] = {
    implicit val timeout: Timeout = Timeout(3 seconds)
    val log = system.log
    
    Behaviors.receiveMessage {
      case ConvertStakeToEUR(tradeData, replyTo) =>
        log.info("Received ConvertStakeToEUR message")
        try {
          val cacheKey = ExchangeRateCache.CacheKey(Currency.withName(tradeData.currency), tradeData.date)

          val exchangeRateFuture: Future[CacheResponse] = exchangeRateCache.ask { replyTo =>
            ExchangeRateCache.GetExchangeRate(cacheKey, replyTo)
          }
          exchangeRateFuture.onComplete {
            case Success(ExchangeRateCache.RateRetrieved(rate)) =>
              log.info("RateRetrieved successful")
              replyTo ! ConvertedStake(formatConversion(tradeData.stake / rate))
            case Success(ExchangeRateCache.RateNotInCache) =>
              log.info("RateNotInCache successful")
              replyTo ! ConversionFailure(ExchangeRateRetrievalError(new Exception("Rate not found in cache")))
            case Failure(ex) =>
              log.error("Unexpected error in ConversionService", ex)
              system.log.error("Unexpected error in ConversionService", ex)
              replyTo ! ConversionFailure(ExchangeRateRetrievalError(ex))
          }
          Behaviors.same
        } catch {
          case e: Exception =>
            system.log.error("Unexpected error in ConversionService", e)
            replyTo ! ConversionFailure(ExchangeRateRetrievalError(e))
            Behaviors.same
        }
    }
  }
  
  private def formatConversion(amount: Double): Double = {
    Try {
      BigDecimal(amount).setScale(5, BigDecimal.RoundingMode.HALF_UP).toDouble
    } match {
      case Success(formattedAmount) => formattedAmount
      case Failure(e) =>
        println(s"Failed to format the amount: $amount due to: ${e.getMessage}")
        amount
    }
  }


}

