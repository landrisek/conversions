package external

import domain.Currency
import java.time.Instant
import scala.util.{Try, Failure}
import com.typesafe.config.Config
import io.circe.parser._

trait ExchangeRateApiClient {
  def apiUrl: String
  def apiKey: String

  def constructRequestUrl(currency: Currency, date: Instant): String
  def parseResponse(response: String, currency: Currency): Try[Double]
}

class MainApiClient(implicit val config: Config) extends ExchangeRateApiClient {
  override val apiUrl = config.getString("app.exchangeAPI")
  // HINT: not used, just matching interface
  override val apiKey = config.getString("app.exchangeAPIKey")
  
  override def constructRequestUrl(currency: Currency, date: Instant): String = {
    s"$apiUrl/${date.toString}?base=EUR"
  }

  override def parseResponse(response: String, currency: Currency): Try[Double] = {
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

class FallbackApiClient(implicit val config: Config) extends ExchangeRateApiClient {
  override val apiUrl = config.getString("app.fallbackExchangeAPI")
  override val apiKey = config.getString("app.fallbackExchangeAPIKey")
  
  override def constructRequestUrl(currency: Currency, date: Instant): String = {
    s"$apiUrl/${date.toString}?base=EUR&access_key=$apiKey"
  }

  override def parseResponse(response: String, currency: Currency): Try[Double] = {
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
