package api

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Success, Failure}

import domain.TradeData
import domain.TradeDataConversion
import serialization.TradeDataJsonProtocol
import service.ConversionService
import service.ConversionService.ConvertStakeToEUR

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

class TradeRoutes(conversionServiceActor: ActorRef[ConversionService.Command])(implicit system: ActorSystem[_], executionContext: ExecutionContext) extends TradeDataJsonProtocol {
  implicit val timeout: Timeout = 3.seconds
  val log = system.log

  val route: Route =
    path("api" / "v1" / "conversion" / "trade") {
      post {
        entity(as[TradeData]) { tradeData =>
          val conversionResult: Future[ConversionService.ConversionResult] = conversionServiceActor.ask(ConvertStakeToEUR(tradeData, _))
          onComplete(conversionResult) {
            case Success(ConversionService.ConvertedStake(convertedAmount)) =>
              val response =TradeDataConversion(
                marketId = tradeData.marketId,
                selectionId = tradeData.selectionId,
                odds = tradeData.odds,
                stake = convertedAmount, // converted stake replaces original stake
                currency = "EUR", // currency is now EUR
                date = tradeData.date.toString
              )
              log.info(s"Processed request: $tradeData, response: $response")
              complete(response)
            case Success(ConversionService.ConversionFailure(reason)) =>
              val response = HttpResponse(StatusCodes.InternalServerError, entity = s"Conversion failed with error: $reason")
              log.error(s"Conversion failed with error: $reason")
              complete(response)
            case Failure(e) =>
              val response = HttpResponse(StatusCodes.InternalServerError, entity = s"Conversion failed with error: ${e.getMessage}")
              log.error(s"Conversion failed with error: ${e.getMessage}", e)
              complete(response)
          }
        }
      }
    }
}
