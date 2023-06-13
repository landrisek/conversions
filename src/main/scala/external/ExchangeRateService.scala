package external

import akka.actor.ClassicActorSystemProvider
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern.CircuitBreaker
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

import domain.Currency

// Define the ExchangeRate interface
trait ExchangeRateServiceTrait {
  def retrieveExchangeRate(currency: Currency, date: Instant): Future[Double]
}

class ExchangeRateService(implicit materializer: Materializer, executionContext: ExecutionContext, system: ClassicActorSystemProvider) extends ExchangeRateServiceTrait {

  private val config = ConfigFactory.load()
  private val exchangeAPI = config.getString("app.exchangeAPI")
  private val fallbackExchangeAPI = config.getString("app.fallbackExchangeAPI")
  private val log: LoggingAdapter = Logging(system.classicSystem, getClass)
  // Circuit Breaker initialization
  private val breaker = new CircuitBreaker(
    system.classicSystem.scheduler,
    maxFailures = 5,
    callTimeout = 10.seconds,
    resetTimeout = 1.minute
  )

  private def retrieveRate(apiUrl: String, currency: Currency, date: Instant): Future[Double] = {
    val request = HttpRequest(uri = s"${apiUrl.replaceAll("\\/latest", s"/${date}")}?symbols=${currency.toString}")
    val responseFuture: Future[HttpResponse] = Http().singleRequest(request)
    log.info(s"Sending request to $request") // log the request

    breaker.withCircuitBreaker(
      responseFuture.flatMap { response =>
        response.status match {
          case StatusCodes.OK =>
            response.entity.toStrict(5.seconds).flatMap { entity =>
              val entityString = entity.data.utf8String
              val json = io.circe.parser.parse(entityString)
              json match {
                case Right(parsedJson) =>
                  log.info(s"Retrieved rates: $parsedJson")
                  val exchangeRates = parsedJson.hcursor.downField("rates").as[Map[String, Double]]
                  exchangeRates match {
                    case Right(rates) => Future.successful(rates.getOrElse(currency.toString, 0.0))
                    case Left(error) =>
                      log.error(s"Failed to parse exchange rates: $error")
                      throw new RuntimeException("Failed to parse exchange rates")
                  }
                case Left(error) =>
                  log.error(s"Failed to parse JSON response: $error")
                  throw new RuntimeException("Failed to parse JSON response")
              }
            }
          case ex =>
            log.error(s"retrieveError failed: $ex")
            throw new RuntimeException(s"Failed to retrieve exchange rates. Status: ${response.status}")
        }
      }
    )
  }

  def retrieveExchangeRate(currency: Currency, date: Instant): Future[Double] = {
    retrieveRate(exchangeAPI, currency, date).recoverWith {
      case ex: Exception =>
        log.error(s"Failed to retrieve exchange rates from main API, attempting to retrieve from fallback API: $ex")
        retrieveRate(fallbackExchangeAPI, currency, date)
    }
  }
}


