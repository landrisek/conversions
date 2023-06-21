package external

import akka.actor.ClassicActorSystemProvider
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import com.typesafe.config.Config
import io.circe._
import io.circe.parser._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Failure}
import java.time.Instant
import domain.Currency

import akka.http.scaladsl.unmarshalling.Unmarshal

trait ExchangeRateApi {
  implicit def executionContext: ExecutionContext
  
  def retrieveExchangeRate(currency: Currency, date: Instant): Future[Double]
  
  protected def constructRequestUrl(currency: Currency, date: Instant): String
  protected def parseResponse(response: String, currency: Currency): Try[Double]
}

class MainApi(implicit val config: Config, implicit val system: ClassicActorSystemProvider, implicit val executionContext: ExecutionContext) extends ExchangeRateApi {
  val apiKey = config.getString("app.mainApi.key")
  val baseUrl = config.getString("app.mainApi.baseUrl")

  override def retrieveExchangeRate(currency: Currency, date: Instant): Future[Double] = {
    val requestUrl = constructRequestUrl(currency, date)
    val request = HttpRequest(uri = requestUrl)
    val responseFuture: Future[HttpResponse] = Http().singleRequest(request)

    responseFuture.flatMap { response =>
      Unmarshal(response.entity).to[String].flatMap { entityString =>
        parseResponse(entityString, currency) match {
          case scala.util.Success(rate) => Future.successful(rate)
          case scala.util.Failure(ex) => Future.failed(ex)
        }
      }
    }
  }
  
  override protected def constructRequestUrl(currency: Currency, date: Instant): String = {
    s"$baseUrl/${date.toString}?symbols=${currency.toString}&access_key=$apiKey"
  }
  
  override protected def parseResponse(response: String, currency: Currency): Try[Double] = {
    parse(response) match {
      case Right(json) =>
        val cursor = json.hcursor
        cursor.downField("rates").downField(currency.toString).as[Double] match {
          case Right(rate) => Try(rate)
          case Left(error) => Failure(new Exception(error.getMessage))
        }
      case Left(error) => Failure(new Exception(error.getMessage))
    }
  }
}

class FallbackApi(implicit val config: Config, implicit val system: ClassicActorSystemProvider, implicit val executionContext: ExecutionContext) extends ExchangeRateApi {
  val apiKey = config.getString("app.fallbackExchangeAPIKey")
  val baseUrl = config.getString("app.fallbackExchangeAPI")

  override def retrieveExchangeRate(currency: Currency, date: Instant): Future[Double] = {
    val requestUrl = constructRequestUrl(currency, date)
    val request = HttpRequest(uri = requestUrl)
    val responseFuture: Future[HttpResponse] = Http().singleRequest(request)

    responseFuture.flatMap { response =>
      Unmarshal(response.entity).to[String].flatMap { entityString =>
        parseResponse(entityString, currency) match {
          case scala.util.Success(rate) => Future.successful(rate)
          case scala.util.Failure(ex) => Future.failed(ex)
        }
      }
    }
  }
  
  override protected def constructRequestUrl(currency: Currency, date: Instant): String = {
    s"$baseUrl/${date.toString}?symbols=${currency.toString}&access_key=$apiKey"
  }
  
    override protected def parseResponse(response: String, currency: Currency): Try[Double] = {
    parse(response) match {
        case Right(json) =>
        val cursor = json.hcursor
        cursor.downField("rates").downField(currency.toString).as[Double] match {
            case Right(rate) => Try(rate)
            case Left(error) => Failure(new Exception(error.getMessage))
        }
        case Left(error) => Failure(new Exception(error.getMessage))
    }
    }

}

