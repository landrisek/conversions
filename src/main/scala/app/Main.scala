import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.stream.Materializer
import scala.io.StdIn
import scala.concurrent.ExecutionContext
import service.ConversionService

import api.TradeRoutes
import cache.ExchangeRateCache
import external.ExchangeRateActor
import external.ExchangeRateService
import external.ExchangeRateServiceTrait

object Main {
  def main(args: Array[String]): Unit = {
    // Create the Actor System
    val typedSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "tradeConversionSystem")
    val classicSystem: akka.actor.ActorSystem = typedSystem.classicSystem

    implicit val materializer: Materializer = Materializer(classicSystem)
    implicit val executionContext: ExecutionContext = classicSystem.dispatcher

    // Create ExchangeRateService
    val exchangeRateService: ExchangeRateService = new ExchangeRateService()(materializer, executionContext, classicSystem)

    // Create ConversionService actor
    val exchangeRateCache: ActorRef[ExchangeRateCache.Command] = typedSystem.systemActorOf(ExchangeRateCache(exchangeRateService), "exchangeRateCache")
    val exchangeRateActor: ActorRef[ExchangeRateActor.Command] = typedSystem.systemActorOf(ExchangeRateActor(exchangeRateService), "exchangeRateActor")

    val conversionServiceActor = typedSystem.systemActorOf(ConversionService(exchangeRateCache)(typedSystem, classicSystem.dispatcher), "conversionServiceActor")

    // Define the HTTP route
    val tradeRoutes = new TradeRoutes(conversionServiceActor)(typedSystem, classicSystem.dispatcher)
    
    // Start the HTTP server
    val bindingFuture = Http()(classicSystem).newServerAt("localhost", 8080).bindFlow(tradeRoutes.route)

    // Print the server address
    println(s"Server online at http://localhost:8080/")

    // Wait for user input to terminate the server
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => classicSystem.terminate())
  }
}
