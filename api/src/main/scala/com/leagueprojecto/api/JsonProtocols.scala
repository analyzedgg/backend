package com.leagueprojecto.api

import com.leagueprojecto.api.domain.Summoner
import spray.json._

trait JsonProtocols extends DefaultJsonProtocol {
  implicit val summonerFormat = jsonFormat5(Summoner.apply)
}
