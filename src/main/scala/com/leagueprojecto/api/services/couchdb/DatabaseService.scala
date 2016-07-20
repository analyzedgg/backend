package com.leagueprojecto.api.services.couchdb

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.CircuitBreaker
import scala.concurrent.duration._
import com.ibm.couchdb.Res.Error
import com.ibm.couchdb.{CouchDb, CouchException, TypeMapping}
import com.leagueprojecto.api.domain.{MatchDetail, Summoner}
import org.http4s.Status.NotFound

import scalaz.{-\/, \/-}

object DatabaseService {
  def props = Props(new DatabaseService)

  case class GetSummoner(region: String, name: String)
  case class SaveSummoner(region: String, summoner: Summoner)
  case class GetMatches(region: String, summonerId: Long, matchIds: Seq[Long])
  case class SaveMatches(region: String, summonerId: Long, matches: Seq[MatchDetail])

  case object NoSummonerFound
  case object SummonerSaved
  case object NoMatchesFound
  case object MatchesSaved
  case class SummonerResult(summoner: Summoner)
  case class MatchesResult(matches: Seq[MatchDetail])
}

class DatabaseService extends Actor with ActorLogging {

  import DatabaseService._
  import context.dispatcher

  private val config = context.system.settings.config

  private val hostname: String = config.getString("couchdb.hostname")
  private val port: Int = config.getInt("couchdb.port")

  val couch = CouchDb(hostname, port)
  val summonerMapping = TypeMapping(classOf[Summoner] -> "Summoner")
  val matchMapping = TypeMapping(classOf[MatchDetail] -> "MatchDetail")
  val summonerDb = couch.db("summoner-db", summonerMapping)
  val matchesDb = couch.db("matches-db", matchMapping)

  val breaker =
    new CircuitBreaker(context.system.scheduler, maxFailures = 5, callTimeout = 10.seconds, resetTimeout = 1.minute)
      .onOpen(whenOpen())


  private[this] def whenOpen(): Unit = {
    println("Circuit breaker open")
  }

  override def receive = {
    case GetSummoner(region, name) =>
      val id = s"$region:$name"

      val summoners = breaker.withSyncCircuitBreaker(summonerDb.docs.get[Summoner](id).attemptRun)

      summoners match {
        case \/-(summonerDoc) =>
          log.info(s"Yay got summoner from Db: $summonerDoc")
          sender() ! SummonerResult(summonerDoc.doc)
        case -\/(CouchException(e: Error)) if e.status == NotFound =>
          log.info(s"No summoner found ($id) from Db")
          sender() ! NoSummonerFound
        case -\/(e) =>
          log.error(s"Error retrieving summoner ($id) from Db")
          println(e)
      }

    case GetMatches(region, summonerId, matchIds) =>
      val id = s"$region:$summonerId"
      val decoratedIds: Seq[String] = matchIds.map(key => s"$id:$key")

      val matches = breaker.withSyncCircuitBreaker(matchesDb.docs.getMany.queryIncludeDocsAllowMissing[MatchDetail](decoratedIds).attemptRun)

      matches match {
        case \/-(matchesDocs) =>
          log.info(s"Yay, got ${matchesDocs.getDocsData.size} matches from Db")
          sender() ! MatchesResult(matchesDocs.getDocsData)
        case -\/(CouchException(e: Error)) if e.status == NotFound =>
          log.info(s"No matches found ($matchIds) from Db")
          sender() ! NoMatchesFound
        case -\/(e) =>
          log.error(s"Error retrieving matches ($matchIds) from Db")
          log.error(s"${e.getMessage}")
          sender() ! NoMatchesFound
      }

    case SaveSummoner(region, summoner) =>
      val id = s"$region:${summoner.name}"

      val saveSummoner = breaker.withSyncCircuitBreaker(summonerDb.docs.create(summoner, id).attemptRun)
      saveSummoner match {
        case \/-(_) =>
          log.info("Yay, summoner saved!")
          sender() ! SummonerSaved
        case -\/(e) =>
          log.error(s"Error saving summoner ($id) in Db")
          println(e)
      }

    case SaveMatches(region, summonerId, matches) =>
      val id = s"$region:$summonerId"

      val matchesById = matches map(m => s"$id:${m.matchId}" -> m) toMap
      val saveMatches = breaker.withSyncCircuitBreaker(matchesDb.docs.createMany(matchesById).attemptRun)

      saveMatches match {
        case \/-(_) =>
          log.info("Yay, matches are saved!")
          sender() ! MatchesSaved
        case -\/(e) =>
          log.error(s"Error saving matches ($id) in Db")
          println(e)
      }
  }
}
