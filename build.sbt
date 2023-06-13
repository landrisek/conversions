name := "Actors"
version := "1.0"
scalaVersion := "2.13.6"


libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.17"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.3.7" exclude("org.slf4j", "slf4j-log4j12")
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.6"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.6"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.17"
libraryDependencies += "io.circe" %% "circe-core" % "0.14.1"
libraryDependencies += "io.circe" %% "circe-generic" % "0.14.1"
libraryDependencies += "io.circe" %% "circe-parser" % "0.14.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.9" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % "10.2.6" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.6.17" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.17" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.6.17"
libraryDependencies += "org.mock-server" % "mockserver-netty" % "5.11.2" % Test

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.14"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.14"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.4"
