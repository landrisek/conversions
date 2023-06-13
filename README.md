# Currency Conversion Service
This service is written in Scala and uses Akka Typed, Akka Http, and Spray-Json. It accepts trade data in JSON format over HTTP POST requests, converts the trade stake from its original currency to EUR, and then returns the updated trade data.

## Structure
The application is broken down into several components:

TradeRoutes: It is responsible for handling HTTP requests. It exposes a POST endpoint at /api/v1/conversion/trade that accepts JSON data with the following format:

{
  "marketId": 123456, 
  "selectionId": 987654, 
  "odds": 2.2, 
  "stake": 253.67, 
  "currency": "USD", 
  "date": "2021-05-18T21:32:42.324Z"
}

The response will have the same format but with the stake converted to EUR based on the rate applicable on the date provided in the request.

Main: It is the entry point for the application, it sets up the Actor System, starts the HTTP server and binds the trade routes.

ExchangeRateCache: It is responsible for caching exchange rates. Exchange rates for a given currency and date are cached in memory for two hours to minimize requests to the external service.

ExchangeRateCacheEntry: This represents an entry in the cache. It holds the exchange rate and maintains a list of actors that are waiting for the rate to be set.

## Prerequisites
sbt
Scala 2.13 or 3
Akka Typed
Akka Http
Spray-Json

## How to run

Clone the repository.
Navigate to the project directory in terminal.
Run the command sbt run.
After starting, the server will be available at http://localhost:8080/. You can test the endpoint using any HTTP client like curl or Postman. A successful request will return a response with the stake amount converted to EUR.
You can send message to it by running `make msg`

Note: The server will continue running until it receives a keyboard input (Enter).

## How to test

Run sbt test.