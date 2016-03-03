package com.leagueprojecto.api.services.couchdb

import akka.actor.{ActorLogging, Props, Actor}
import com.ibm.couchdb.Res.Error
import com.ibm.couchdb.{CouchException, TypeMapping, CouchDb}
import com.leagueprojecto.api.domain.{MatchDetail, Summoner}
import org.http4s.Status.NotFound

import scalaz.{\/-, -\/}

object DatabaseService {
  def props = Props(new DatabaseService)

  case class GetSummoner(region: String, name: String)

  case class SaveSummoner(region: String, summoner: Summoner)

  case class GetMatches(region: String, summonerId: Long, matchIds: Seq[Long])

  case class SaveMatches(region: String, summonerId: Long, matches: Seq[MatchDetail])


  case object NoSummonerFound

  case object SummonerSaved

  case object MatchesSaved

}

class DatabaseService extends Actor with ActorLogging {

  import DatabaseService._

  private val config = context.system.settings.config

  private val hostname: String = config.getString("couchdb.hostname")
  private val port: Int = config.getInt("couchdb.port")

  val couch = CouchDb(hostname, port)
  val summonerMapping = TypeMapping(classOf[Summoner] -> "Summoner")
  val matchMapping = TypeMapping(classOf[MatchDetail] -> "MatchDetail")
  val summonerDb = couch.db("summoner-db", summonerMapping)
  val matchesDb = couch.db("matches-db", matchMapping)

  override def receive = {
    case GetSummoner(region, name) =>
      val id = s"$region:$name"

      summonerDb.docs.get[Summoner](id).attemptRun match {
        case \/-(summonerDoc) =>
          log.info(s"Yay got summoner from Db: $summonerDoc")
          sender() ! summonerDoc.doc
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
      matchesDb.docs.getMany.queryIncludeDocsAllowMissing[MatchDetail](decoratedIds).attemptRun match {
        case \/-(matchesDocs) =>
          log.info(s"Yay, got ${matchesDocs.getDocsData.size} matches from Db")
          sender() ! matchesDocs.getDocsData
        case -\/(e) =>
          log.error(s"Error retrieving matches ($matchIds) from Db")
          e.printStackTrace()
      }

    case SaveSummoner(region, summoner) =>
      val id = s"$region:${summoner.name}"

      summonerDb.docs.create(summoner, id).attemptRun match {
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

      matchesDb.docs.createMany(matchesById).attemptRun match {
        case \/-(_) =>
          log.info("Yay, matches are saved!")
          sender() ! MatchesSaved
        case -\/(e) =>
          log.error(s"Error saving matches ($id) in Db")
          println(e)
      }
  }
}
