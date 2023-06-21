package external

import akka.actor.ClassicActorSystemProvider
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern.CircuitBreaker
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import java.time.Instant
import java.util.concurrent.TimeoutException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

import akka.http.scaladsl.unmarshalling.Unmarshal
import domain.Currency

// Define the ExchangeRate interface
trait ExchangeRateServiceTrait {
  def retrieveExchangeRate(currency: Currency, date: Instant): Future[Double]
}

class ExchangeRateService(apiClient: ExchangeRateApiClient, fallbackApiClient: ExchangeRateApiClient)
                         (implicit materializer: Materializer, executionContext: ExecutionContext, system: ClassicActorSystemProvider)
  extends ExchangeRateServiceTrait {

  private val log: LoggingAdapter = Logging(system.classicSystem, getClass)

  private val breaker = new CircuitBreaker(
    system.classicSystem.scheduler,
    maxFailures = 5,
    callTimeout = 10.seconds,
    resetTimeout = 1.minute
  )

  private def retrieveRate(client: ExchangeRateApiClient, currency: Currency, date: Instant): Future[Double] = {
    val requestUrl = client.constructRequestUrl(currency, date)
    val request = HttpRequest(uri = requestUrl)

    breaker.withCircuitBreaker(
      for {
        response <- Http().singleRequest(request)
        entityString <- Unmarshal(response.entity).to[String]
        rate <- client.parseResponse(entityString, currency) match {
          case Success(rate) => Future.successful(rate)
          case Failure(ex) => Future.failed(ex)
        }
      } yield rate
    )
  }


  override def retrieveExchangeRate(currency: Currency, date: Instant): Future[Double] = {
    retrieveRate(apiClient, currency, date).recoverWith {
      case ex: Exception =>
        log.error(s"Failed to retrieve exchange rates from main API, error: $ex. Attempting to retrieve from fallback API.")
        retrieveRate(fallbackApiClient, currency, date)
    }
  }
}


