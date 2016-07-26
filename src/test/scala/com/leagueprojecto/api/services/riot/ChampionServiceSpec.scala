package com.leagueprojecto.api.services.riot

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{GivenWhenThen, Matchers, WordSpecLike}

class ChampionServiceSpec extends TestKit(ActorSystem("ChampionServiceTest")) with ImplicitSender with WordSpecLike with Matchers with GivenWhenThen {

  "A ChampionService" must {

  }

}
