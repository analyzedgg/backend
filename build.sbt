name := "api"

organization := "com.leagueprojecto"

version := "0.0.1"

scalaVersion := "2.11.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Mvn repository" at "http://mvnrepository.com/artifact/"

libraryDependencies ++= {
  val akkaVersion = "2.3.10"
  val kamonVersion = "0.6.0"
  val akkaStreamVersion = "1.0"
  val scalaTestVersion = "2.2.1"
  val logbackVersion = "1.1.2"
  val jacksonVersion: String = "2.7.4"
  val couchDbScalaVersion: String = "0.7.0"

  Seq(
    "io.kamon" %% "kamon-core" % kamonVersion,
    "io.kamon" %% "kamon-scala" % kamonVersion,
    "io.kamon" %% "kamon-akka" % kamonVersion,
    "io.kamon" %% "kamon-log-reporter" % kamonVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-core-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaStreamVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion % "runtime",
    "com.typesafe.akka" % "akka-testkit_2.11" % akkaVersion % "test,it",
    "com.typesafe.akka" %% "akka-http-testkit-experimental" % akkaStreamVersion % "test,it",
    "org.scalatest"     %% "scalatest" % scalaTestVersion % "test,it",
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
    "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % jacksonVersion,
    "com.ibm"           %% "couchdb-scala" % couchDbScalaVersion
  )
}
aspectjSettings

fork in run := true

javaOptions in run <++= AspectjKeys.weaverOptions in Aspectj

mainClass in(Compile, run) := Some("com.leagueprojecto.api.Startup")
