package com.analyzedgg.api.domain.riot

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonNode}
import com.analyzedgg.api.domain.Summoner

case class RiotSummoner(summoner: Summoner)

class RiotSummonerDeserializer extends JsonDeserializer[RiotSummoner] {

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): RiotSummoner = {
    val node: JsonNode = jp.getCodec.readTree(jp)
    val summoner = node.fields.next().getValue
    val id = summoner.get("id").asLong()
    val name = summoner.get("name").asText()
    val profileIconId = summoner.get("profileIconId").asLong()
    val revisionDate = summoner.get("revisionDate").asLong()
    val summonerLevel = summoner.get("summonerLevel").asInt()

    RiotSummoner(Summoner(id, name, profileIconId, revisionDate, summonerLevel))
  }

}
