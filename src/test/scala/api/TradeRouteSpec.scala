package api

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.actor.typed.ActorSystem
import java.time.Instant
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.Future

import api.TradeRoutes
import domain.{Currency, TradeData}
import service.{ConversionService}
import cache.{ExchangeRateCache}

class TradeRouteSpec extends AnyWordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll {

  val testKit = ActorTestKit()

  implicit def typedSystem: ActorSystem[Nothing] = testKit.system

  // Mocked ExchangeRateCache
  val exchangeRateCacheActor = testKit.spawn(Behaviors.receiveMessage[ExchangeRateCache.Command] {
    case ExchangeRateCache.GetExchangeRate(_, replyTo) =>
      replyTo ! ExchangeRateCache.RateRetrieved(1.0)
      Behaviors.same
    case _ => Behaviors.same
  })

  val conversionServiceActor = testKit.spawn(ConversionService(exchangeRateCacheActor), "conversionService")

  val routes = new TradeRoutes(conversionServiceActor).route

  "TradeRoutes" should {
    "convert trade data to EUR on POST requests to /api/v1/conversion/trade" in {
      // Prepare a request with some TradeData
      val request = Post("/api/v1/conversion/trade").withEntity(ContentTypes.`application/json`,
        """{
          | "marketId": 1,
          | "selectionId": 2,
          | "odds": 1.5,
          | "stake": 100,
          | "currency": "USD",
          | "date": "2023-01-01T00:00:00Z"
          |}""".stripMargin)

      // Test the route
      request ~> routes ~> check {
        status shouldBe StatusCodes.OK
        // Add more checks for the response here...
      }
    }
  }

  // Remember to shut down the ActorTestKit
  override def afterAll(): Unit = testKit.shutdownTestKit()

}
