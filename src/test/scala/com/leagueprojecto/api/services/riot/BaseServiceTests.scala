package com.leagueprojecto.api.services.riot

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{GivenWhenThen, Matchers, WordSpecLike}

import scala.io.Source

abstract class BaseServiceTests(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers with GivenWhenThen {
  def readFileFromClasspath(file: String): String =
    Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(file)).mkString
}
