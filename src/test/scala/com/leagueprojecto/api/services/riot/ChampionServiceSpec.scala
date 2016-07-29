package com.leagueprojecto.api.services.riot

import akka.actor.ActorSystem
import akka.http.javadsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.{GivenWhenThen, Matchers, WordSpecLike}

import scala.concurrent.Future

class ChampionServiceSpec extends TestKit(ActorSystem("ChampionServiceTest")) with ImplicitSender with WordSpecLike with Matchers with GivenWhenThen {

/*
  val sut = TestActorRef(new ChampionService{
    override riotRequest(HttpRequest(), "global"): Future[HttpResponse] =
    Future(Ok)
  })

  "A ChampionService" must {

  }
*/

}
