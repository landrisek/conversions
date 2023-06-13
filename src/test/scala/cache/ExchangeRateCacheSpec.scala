package cache

import akka.actor.typed.ActorSystem
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import cache.ExchangeRateCache._
import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

import domain.Currency
import external.ExchangeRateServiceTrait

class ExchangeRateCacheSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  val testKit = ActorTestKit()
  implicit val system: ActorSystem[Nothing] = testKit.system
  implicit val ec: ExecutionContext = system.executionContext

  // Stub for ExchangeRateService
  val exchangeRateServiceStub = new ExchangeRateServiceTrait {
    override def retrieveExchangeRate(currency: Currency, date: Instant): Future[Double] = Future.successful(1.0)
  }

  val exchangeRateCache = testKit.spawn(ExchangeRateCache(exchangeRateServiceStub), "exchangeRateCache")

  "The ExchangeRateCache actor" should {
    "retrieve exchange rates and return RateRetrieved when it gets a GetExchangeRate command" in {
      val probe = testKit.createTestProbe[CacheResponse]()
      val cacheKey = CacheKey(Currency.USD, Instant.now())
      exchangeRateCache ! GetExchangeRate(cacheKey, probe.ref)

      probe.expectMessage(RateRetrieved(1.0))
    }

    "evict old data when it gets an EvictKey command" in {
      val probe = testKit.createTestProbe[CacheResponse]()
      val cacheKey = CacheKey(Currency.USD, Instant.now())
      exchangeRateCache ! GetExchangeRate(cacheKey, probe.ref)
      
      // Wait for the rate to be fetched and cached
      probe.expectMessage(RateRetrieved(1.0))

      // Wait a bit before evicting the key
      Thread.sleep(1000)
      exchangeRateCache ! EvictKey(cacheKey)

      // Wait a bit before trying to get the rate again
      Thread.sleep(1000)
      exchangeRateCache ! GetExchangeRate(cacheKey, probe.ref)

      // Since the rate has been evicted, actor should again hit the external service and get the rate
      probe.expectMessage(RateRetrieved(1.0))
    }
  }

  override def afterAll(): Unit = testKit.shutdownTestKit()
}
