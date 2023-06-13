package cache

import akka.actor.typed.ActorSystem
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import scala.concurrent.ExecutionContext
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import cache.ExchangeRateCacheEntry._
import domain.Currency

class ExchangeRateCacheEntrySpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  val testKit = ActorTestKit()
  implicit val system: ActorSystem[Nothing] = testKit.system
  implicit val ec: ExecutionContext = system.executionContext

  val exchangeRateCacheEntry = testKit.spawn(ExchangeRateCacheEntry(), "exchangeRateCacheEntry")

  "The ExchangeRateCacheEntry actor" should {
    "set rate and return RateRetrieved when it gets a SetRate command followed by a GetRate command" in {
      val probe = testKit.createTestProbe[ExchangeRateCache.CacheResponse]()
      exchangeRateCacheEntry ! SetRate(1.0)
      exchangeRateCacheEntry ! GetRate(probe.ref)

      probe.expectMessage(ExchangeRateCache.RateRetrieved(1.0))
    }

    "maintain the rate even if it gets multiple GetRate commands" in {
      val probe = testKit.createTestProbe[ExchangeRateCache.CacheResponse]()
      exchangeRateCacheEntry ! GetRate(probe.ref)
      exchangeRateCacheEntry ! GetRate(probe.ref)

      probe.expectMessage(ExchangeRateCache.RateRetrieved(1.0))
      probe.expectMessage(ExchangeRateCache.RateRetrieved(1.0))
    }

    "warn if it gets a SetRate command in the cached state" in {
      val probe = testKit.createTestProbe[ExchangeRateCache.CacheResponse]()
      exchangeRateCacheEntry ! SetRate(2.0)

      probe.expectNoMessage()
    }
  }

  override def afterAll(): Unit = testKit.shutdownTestKit()
}
