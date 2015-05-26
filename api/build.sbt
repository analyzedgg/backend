name := "api"

organization := "com.leagueprojecto"

version := "0.0.1"

scalaVersion := "2.11.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Mvn repository" at "http://mvnrepository.com/artifact/"

libraryDependencies ++= {
  val akkaVersion = "2.3.10"
  val akkaStreamVersion = "1.0-M5"
  val scalaTestVersion = "2.2.1"
  val logbackVersion = "1.1.2"
  val asyncHttpClientVersion = "1.9.25"

  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-core-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaStreamVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "com.typesafe.akka" %% "akka-http-testkit-experimental" % akkaStreamVersion % "test",
    "org.scalatest"     %% "scalatest" % scalaTestVersion % "test",
    "com.ning"          % "async-http-client" % asyncHttpClientVersion
  )
}

mainClass in (Compile,run) := Some("com.leagueprojecto.api.Startup")

fork in run := true
