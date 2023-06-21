package cache

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.ActorContext
import domain.Currency
import java.time.Instant
import external.{ExchangeRateService, ExchangeRateServiceTrait}

object ExchangeRateCacheEntry {

  sealed trait Command
  case class SetRate(rate: Double) extends Command
  case class GetRate(replyTo: ActorRef[ExchangeRateCache.CacheResponse]) extends Command
  case object RateRetrievalFailed extends Command

  case class State(rate: Option[Double], pending: List[ActorRef[ExchangeRateCache.CacheResponse]])

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    uninitialized(context, State(None, Nil))
  }

  private def uninitialized(context: ActorContext[Command], state: State): Behavior[Command] = Behaviors.receive { 
    (_, message) =>
      message match {
        case SetRate(rate) => 
          state.pending.foreach(_ ! ExchangeRateCache.RateRetrieved(rate))
          cached(context, State(Some(rate), Nil))
        case GetRate(replyTo) =>
          uninitialized(context, State(state.rate, replyTo :: state.pending))
        case RateRetrievalFailed =>
          state.pending.foreach(_ ! ExchangeRateCache.RateNotInCache)
          failureState(context) // transitioning to a failure state
      }
  }

  private def failureState(context: ActorContext[Command]): Behavior[Command] = Behaviors.receive {
    (_, message) =>
      message match {
        case GetRate(replyTo) => 
          replyTo ! ExchangeRateCache.RateNotInCache
          Behaviors.same
        case _ => 
          context.log.warn("Received inappropriate command in 'failureState'. This indicates a problem in the program.")
          Behaviors.same
      }
  }

  private def cached(context: ActorContext[Command], state: State): Behavior[Command] = Behaviors.receive {
    (_, message) =>
      message match {
        case GetRate(replyTo) =>
          state.rate match {
            case Some(rate) => 
              replyTo ! ExchangeRateCache.RateRetrieved(rate)
              Behaviors.same
            case None => 
              uninitialized(context, State(state.rate, replyTo :: state.pending))
          }
        case SetRate(rate) => 
          context.log.warn("Received SetRate command in 'cached' state. This indicates a problem in the program.")
          Behaviors.same
        case RateRetrievalFailed => 
          context.log.warn("Received RateRetrievalFailed command in 'cached' state. This indicates a problem in the program.")
          Behaviors.same
      }
  }
}

