package external

import akka.actor.ClassicActorSystemProvider
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.stream.Materializer
import akka.actor.typed.{ActorRef, Behavior}
import java.time.Instant

import scala.concurrent.{ExecutionContext}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

import domain.Currency

object ExchangeRateActor {
  sealed trait Command
  case class GetExchangeRateByDate(currency: Currency, date: Instant, replyTo: ActorRef[Double]) extends Command

  def apply(exchangeRateService: ExchangeRateService): Behavior[Command] = Behaviors.setup { context =>
    implicit val materializer: Materializer = Materializer(context.system.toClassic)
    implicit val executionContext: ExecutionContext = context.system.executionContext

    Behaviors.receiveMessage {
      case GetExchangeRateByDate(currency, date, replyTo) =>
        exchangeRateService.retrieveExchangeRate(currency, date).onComplete {
          case Success(rate) => replyTo ! rate
          case Failure(ex) => context.log.error(s"Failed to retrieve exchange rate for ${currency}: ${ex.getMessage}")
        }
        Behaviors.same
    }
  }
}
