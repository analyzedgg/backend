package com.leagueprojecto.api.services

import akka.actor.Status.Failure
import akka.actor.{ActorLogging, ActorRef, FSM, Props}
import com.leagueprojecto.api.domain.Summoner
import com.leagueprojecto.api.services.SummonerManager._
import com.leagueprojecto.api.services.couchdb.DatabaseService
import com.leagueprojecto.api.services.couchdb.DatabaseService.{NoSummonerFound, SummonerSaved}
import com.leagueprojecto.api.services.riot.SummonerService
import com.leagueprojecto.api.services.riot.SummonerService.GetSummonerByName

object SummonerManager {

  sealed trait State
  case object Idle extends State
  case object RetrievingFromDb extends State
  case object RetrievingFromRiot extends State
  case object PersistingToDb extends State

  case class GetSummoner(region: String, name: String)
  case class RequestData(sender: ActorRef, getSummoner: GetSummoner)

  case class Result(summoner: Summoner)

  def props = Props[SummonerManager]
}

class SummonerManager extends FSM[State, (Option[RequestData], Option[Summoner])] with ActorLogging {

  val dbService = context.actorOf(DatabaseService.props)

  startWith(Idle, (None, None))

  when(Idle) {
    case Event(message: GetSummoner, _) =>
      goto(RetrievingFromDb) using(Some(RequestData(sender(), message)), None)
  }

  when(RetrievingFromDb) {
    case Event(summoner: Summoner, (Some(RequestData(sender, _)), _)) =>
      sender ! Result(summoner)
      stop()
    case Event(DatabaseService.NoSummonerFound, stateData) =>
      goto(RetrievingFromRiot) using stateData
  }

  when(RetrievingFromRiot) {
    case Event(summoner: Summoner, (Some(RequestData(sender, _)), _)) =>
      sender ! Result(summoner)
      goto(PersistingToDb) using stateData.copy(_2 = Some(summoner))
    case Event(notFound: SummonerService.SummonerNotFound, (Some(RequestData(sender, _)), _)) =>
      sender ! Failure(notFound)
      stop()
    case Event(failure: Failure, (Some(RequestData(sender, _)), _)) =>
      sender ! failure
      stop()
  }

  when(PersistingToDb) {
    case Event(SummonerSaved, _) => stop()
  }

  onTransition {
    case Idle -> RetrievingFromDb =>
      nextStateData match {
        case (Some(RequestData(_, GetSummoner(region, name))), None) =>
          dbService ! DatabaseService.GetSummoner(region, name)

        case failData =>
          log.error(s"Something went wrong when going from Idle -> RetrievingFromDb, got data: $failData")
      }

    case RetrievingFromDb -> RetrievingFromRiot =>
      nextStateData match {
        case (Some(RequestData(_, GetSummoner(region, name))), None) =>
          val summonerRef = context.actorOf(SummonerService.props(region, name))
          summonerRef ! GetSummonerByName

        case failData =>
          log.error(s"Something went wrong when going from RetrievingFromDb -> RetrievingFromRiot, got data: $failData")
      }

    case RetrievingFromRiot -> PersistingToDb =>
      nextStateData match {
        case (Some(RequestData(_, GetSummoner(region, _))), Some(summoner)) =>
          dbService ! DatabaseService.SaveSummoner(region, summoner)

        case failData =>
          log.error(s"Something went wrong when going from RetrievingFromRiot -> PersistingToDb, got data: $failData")
      }
  }

  whenUnhandled {
    case Event(msg, _) =>
      log.error(s"Got unhandled message ($msg) in $stateName")
      stop()
  }
}

