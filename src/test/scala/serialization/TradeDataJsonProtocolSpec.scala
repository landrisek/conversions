package serialization

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import java.time.Instant
import java.time.format.DateTimeParseException
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat, deserializationError}

import domain.{TradeData, TradeDataConversion}

trait TradeDataJsonProtocolSpec extends DefaultJsonProtocol with SprayJsonSupport {
  implicit object InstantJsonFormat extends RootJsonFormat[Instant] {
    def write(obj: Instant): JsValue = JsString(obj.toString)

    def read(json: JsValue): Instant = json match {
      case JsString(str) => try {
        Instant.parse(str)
      } catch {
        case e: DateTimeParseException => deserializationError("Error parsing date '" + str + "'. Expected format is ISO_INSTANT (e.g., '2011-12-03T10:15:30Z')", e)
      }
      case _ => deserializationError("Expected JsString")
    }
  }

  implicit val tradeDataFormat: RootJsonFormat[TradeData] = jsonFormat6(TradeData)
  implicit val tradeDataConversionFormat: RootJsonFormat[TradeDataConversion] = jsonFormat6(TradeDataConversion)
}
