package cache

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{Actor, Cancellable, ClassicActorSystemProvider}
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

  private val config = ConfigFactory.load()
  private val ttl = config.getDuration("app.cacheTTL").toMillis.milliseconds

def apply(exchangeRateService: ExchangeRateServiceTrait): Behavior[Command] = Behaviors.setup { context =>
    println("ExchangeRateCache started")
    cacheBehavior(context, Map.empty, Map.empty, exchangeRateService)
  }

  private def cacheBehavior(context: ActorContext[Command], cache: Map[CacheKey, ActorRef[ExchangeRateCacheEntry.Command]], evictors: Map[CacheKey, Cancellable], exchangeRateService: ExchangeRateServiceTrait): Behavior[Command] = {
    implicit val executionContext: ExecutionContext = context.executionContext

    Behaviors.receiveMessage {
      case GetExchangeRate(key, replyTo) =>
        cache.get(key) match {
          case Some(cachedRateActor) =>
            evictors.get(key).foreach(_.cancel())
            cachedRateActor ! ExchangeRateCacheEntry.GetRate(replyTo)
            val newEvictor = context.scheduleOnce(ttl, context.self, EvictKey(key))
            cacheBehavior(context, cache, evictors + (key -> newEvictor), exchangeRateService)
          case None =>
            val rateActor = context.spawn(ExchangeRateCacheEntry(), key.toString)
            exchangeRateService.retrieveExchangeRate(key.currency, key.date).onComplete {
              case Success(rate) => rateActor ! ExchangeRateCacheEntry.SetRate(rate)
              case Failure(ex) => context.log.error(s"Failed to retrieve exchange rate for ${key.currency}: ${ex.getMessage}")
            }
            rateActor ! ExchangeRateCacheEntry.GetRate(replyTo)
            val evictor = context.scheduleOnce(ttl, context.self, EvictKey(key))
            cacheBehavior(context, cache + (key -> rateActor), evictors + (key -> evictor), exchangeRateService)
        }

      case EvictKey(key) =>
        cache.get(key).foreach(context.stop)
        evictors.get(key).foreach(_.cancel())
        cacheBehavior(context, cache - key, evictors - key, exchangeRateService)
    }
  }

}
