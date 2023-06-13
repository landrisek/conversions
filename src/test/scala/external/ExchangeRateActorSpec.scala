package external

import akka.actor.typed.ActorSystem
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import external.ExchangeRateActor._
import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import domain.Currency

class ExchangeRateActorSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  val testKit = ActorTestKit()
  implicit val system: ActorSystem[Nothing] = testKit.system
  implicit val ec: ExecutionContext = system.executionContext

  // Stub for ExchangeRateService
  val exchangeRateServiceStub = new ExchangeRateService {
    override def retrieveExchangeRate(currency: Currency, date: Instant): Future[Double] = Future.successful(1.0)
  }

  val exchangeRateActor = testKit.spawn(ExchangeRateActor(exchangeRateServiceStub), "exchangeRateActor")

  "The ExchangeRateActor" should {
    "retrieve exchange rate and return the rate when it gets a GetExchangeRateByDate command" in {
      val probe = testKit.createTestProbe[Double]()
      exchangeRateActor ! GetExchangeRateByDate(Currency.USD, Instant.now(), probe.ref)

      probe.expectMessage(1.0)
    }
  }

  override def afterAll(): Unit = testKit.shutdownTestKit()
}
