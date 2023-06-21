package cache

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{Actor, ClassicActorSystemProvider}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import domain.Currency
import java.time.Instant
import external.{ExchangeRateService, ExchangeRateServiceTrait}

object ExchangeRateCache {

  sealed trait CacheResponse
  case class RateRetrieved(rate: Double) extends CacheResponse
  case object RateNotInCache extends CacheResponse

  case class CacheKey(currency: Currency, date: Instant) {
    override def toString: String = s"${currency}_${date.toString.replaceAll("[-:.]", "_")}"
  }

  sealed trait Command
  case class GetExchangeRate(key: CacheKey, replyTo: ActorRef[CacheResponse]) extends Command
  case class EvictKey(key: CacheKey) extends Command
  case class RateRetrievalFailed(key: CacheKey) extends Command

  private val config = ConfigFactory.load()
  private val ttl = config.getDuration("app.cacheTTL").toMillis.milliseconds

  def apply(exchangeRateService: ExchangeRateServiceTrait): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.withTimers { timers =>
      println("ExchangeRateCache started")
      cacheBehavior(context, Map.empty, exchangeRateService, timers)
    }
  }

  private def cacheBehavior(context: ActorContext[Command], cache: Map[CacheKey, ActorRef[ExchangeRateCacheEntry.Command]], exchangeRateService: ExchangeRateServiceTrait, timers: TimerScheduler[Command]): Behavior[Command] = {
    implicit val executionContext: ExecutionContext = context.executionContext

    Behaviors.receiveMessage {
      case GetExchangeRate(key, replyTo) =>
        cache.get(key) match {
          case Some(cachedRateActor) =>
            cachedRateActor ! ExchangeRateCacheEntry.GetRate(replyTo)
            cacheBehavior(context, cache, exchangeRateService, timers)
         case None =>
          val rateActor = context.spawn(ExchangeRateCacheEntry(), key.toString)
          exchangeRateService.retrieveExchangeRate(key.currency, key.date).onComplete {
            case Success(rate) => rateActor ! ExchangeRateCacheEntry.SetRate(rate)
            case Failure(ex) => 
              context.log.error(s"Failed to retrieve exchange rate for ${key.currency}: ${ex.getMessage}")
              rateActor ! ExchangeRateCacheEntry.RateRetrievalFailed
          }
          rateActor ! ExchangeRateCacheEntry.GetRate(replyTo)
          timers.startSingleTimer(EvictKey(key), ttl)
          cacheBehavior(context, cache + (key -> rateActor), exchangeRateService, timers)
        }
      case RateRetrievalFailed(key) =>
        cache.get(key).foreach(_ ! ExchangeRateCacheEntry.RateRetrievalFailed)
        cacheBehavior(context, cache - key, exchangeRateService, timers)
      case EvictKey(key) =>
        cache.get(key).foreach(context.stop)
        cacheBehavior(context, cache - key, exchangeRateService, timers)
    }
  }
}
