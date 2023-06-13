import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import java.time.Instant
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import cache.ExchangeRateCache
import domain.{Currency, TradeData}
import service.ConversionService

class ConversionServiceSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "testSystem")
  implicit val executionContext = system.executionContext
  val testKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "ConversionService actor" should "reply with ConvertedStake when rate is retrieved from the cache" in {
    val replyProbe = testKit.createTestProbe[ConversionService.ConversionResult]()

    // Creating a mock cache that always returns a fixed exchange rate
    val mockCache = testKit.spawn(Behaviors.receiveMessage[ExchangeRateCache.Command] { message =>
      message match {
        case ExchangeRateCache.GetExchangeRate(_, replyTo) =>
          replyTo ! ExchangeRateCache.RateRetrieved(0.85)
          Behaviors.same
      }
    })

    val conversionService = testKit.spawn(ConversionService(mockCache))

    val tradeData = TradeData(123, 456, 1.5, 50.0, "USD", Instant.now)
    conversionService ! ConversionService.ConvertStakeToEUR(tradeData, replyProbe.ref)

    // 50.0 / 0.85 after conversion
    val expectedConvertedStake = 58.82353
    replyProbe.expectMessage(ConversionService.ConvertedStake(expectedConvertedStake))
  }
}
