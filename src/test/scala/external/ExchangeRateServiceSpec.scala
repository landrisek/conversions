package external

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.stream.Materializer
import akka.stream.ActorMaterializer
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent.ExecutionContext
import com.typesafe.config.ConfigFactory

import domain.Currency
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import java.time.Instant

import scala.concurrent.Await
import scala.concurrent.duration._
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.integration.ClientAndServer

class ExchangeRateServiceSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  val mockServer: ClientAndServer = ClientAndServer.startClientAndServer(1080)
  implicit val system: ActorSystem = ActorSystem("testSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  override def afterAll(): Unit = {
    mockServer.stop()
    system.terminate()
  }

  "ExchangeRateService" should {
    "retrieve exchange rates" in {
      // setup MockServer to respond to the expected request
      val date = "2021-05-18"
      mockServer
        .when(
          HttpRequest.request()
            .withMethod("GET")
            .withPath(s"/$date")
        )
        .respond(
          HttpResponse.response()
            .withBody("""{ "rates": { "USD": 1.0 } }""")
        )
        
      val config = ConfigFactory.load()
      
      val mainApiClient = new MainApiClient()(config)
      val fallbackApiClient = new FallbackApiClient()(config)

      // Use the real ExchangeRateService, but it will talk to our mock server instead of the real one
      val service = new ExchangeRateService(mainApiClient, fallbackApiClient)

      val instant = Instant.parse("2021-05-18T21:32:42.324Z")
      val futureRate = service.retrieveExchangeRate(Currency.USD, instant)

      // Await the completion of the future for testing. In production code, you should never block on a future like this.
      val rate = Await.result(futureRate, 5.seconds)

      // Now we can make assertions on the rate
      rate shouldBe (1.222385)

      // Verify that MockServer received exactly one request
      val recordedRequests = mockServer.retrieveRecordedRequests(HttpRequest.request())
    }
  }
}
