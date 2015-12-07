package com.leagueprojecto.api.services.couchdb

import akka.actor.{ActorLogging, Props, Actor}
import akka.stream.{ActorMaterializer, Materializer}
import com.ibm.couchdb.Res.Error
import com.ibm.couchdb.{CouchException, TypeMapping, CouchDb}
import com.leagueprojecto.api.domain.{MatchHistory, Summoner}
import org.http4s.Status.NotFound

import scala.concurrent.ExecutionContextExecutor
import scalaz.{\/-, -\/}

object DatabaseService {
  def props = Props(new DatabaseService)

  case class GetSummoner(region: String, name: String)

  case class SaveSummoner(region: String, summoner: Summoner)

  case class GetMatches(region: String, summonerId: Long, matchIds: Seq[Long])


  case object NoSummonerFound

  case object SummonerSaved

}

class DatabaseService extends Actor with ActorLogging {

  import DatabaseService._

  private val config = context.system.settings.config

  private val hostname: String = config.getString("couchdb.hostname")
  private val port: Int = config.getInt("couchdb.port")

  val couch = CouchDb(hostname, port)
  val summonerMapping = TypeMapping(classOf[Summoner] -> "Summoner")
  val matchMapping = TypeMapping(classOf[MatchHistory] -> "MatchHistory")
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
      matchesDb.docs.getMany[MatchHistory](matchIds.map(key => s"$id:$key")).attemptRun match {
        case \/-(matchesDocs) =>
          sender() ! matchesDocs.getDocsData
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

  }
}
