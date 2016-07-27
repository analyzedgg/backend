package com.leagueprojecto.api.services.riot

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.{GivenWhenThen, Matchers, WordSpecLike}

class ChampionServiceSpec extends TestKit(ActorSystem("ChampionServiceTest")) with ImplicitSender with WordSpecLike with Matchers with GivenWhenThen {

  val sut = TestActorRef(new ChampionService{

  })

  "A ChampionService" must {

  }

}
