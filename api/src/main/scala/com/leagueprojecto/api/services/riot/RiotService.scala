package com.leagueprojecto.api.services.riot

import akka.actor.Actor
import com.ning.http.client.AsyncHttpClient

trait RiotService {
  this: Actor =>

  private val config = context.system.settings.config
  private val endpoint = config.getString("riot.api-endpoint")

  val httpClient: AsyncHttpClient = new AsyncHttpClient
  val api_key = config.getString("riot.api-key")

  // Services
  val summoner_byname = config.getString("riot.service-summonerbyname")

  def riotApi(region: String, service: String) = s"$endpoint/$region/$service"
}
