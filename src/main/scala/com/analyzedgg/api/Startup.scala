package com.analyzedgg.api

import akka.actor._
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

object Startup extends App with Routes {
  override implicit val system: ActorSystem = ActorSystem("api")
  override implicit val executor = system.dispatcher
  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  implicit val materializer = ActorMaterializer()

  // Bind the HTTP endpoint. Specify http.interface and http.port in the configuration
  // to change the address and port to bind to.
  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
