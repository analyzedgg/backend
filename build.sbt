name := "api"

organization := "com.leagueprojecto"

version := "0.0.1"

scalaVersion := "2.11.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Mvn repository" at "http://mvnrepository.com/artifact/"

libraryDependencies ++= {
  val akkaVersion = "2.3.10"
  val akkaStreamVersion = "1.0"
  val scalaTestVersion = "2.2.1"
  val logbackVersion = "1.1.2"
  val jacksonVersion: String = "2.7.4"
  val couchDbScalaVersion: String = "0.7.0"

  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-core-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaStreamVersion,
    "ch.qos.logback"    % "logback-classic" % logbackVersion % "runtime",
    "com.typesafe.akka" % "akka-testkit_2.11" % akkaVersion % "test,it",
    "com.typesafe.akka" %% "akka-http-testkit-experimental" % akkaStreamVersion % "test,it",
    "org.scalatest"     %% "scalatest" % scalaTestVersion % "test,it",
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
    "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % jacksonVersion,
    "com.ibm"           %% "couchdb-scala" % couchDbScalaVersion
  )
}

val deployTask = TaskKey[Unit]("deploy", "Copies assembly jar to remote location")

deployTask <<= assembly map { (asm) =>
  val account = "pi@192.168.178.12"
  val relativeLocal = new File("build.sbt").getAbsoluteFile.getParentFile
  val local = asm.getAbsoluteFile.relativeTo(relativeLocal).get.toString
  val remote = account + ":" + "league/" + asm.getName
  println(s"Copying: $local -> $remote")
  s"scp $local $remote".!

  println("Run deployed app")
  s"ssh $account cd league && ./run.sh".!
}

mainClass in (Compile,run) := Some("com.leagueprojecto.api.Startup")
coverageEnabled := true

fork in run := true
