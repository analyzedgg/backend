package com.leagueprojecto.api.domain

case class ChampionList(
                       data: Map[String, Champion],
                       `type`: String,
                       version: String
                       )
case class Champion(
                   tags: Seq[String],
                   id: Int,
                   title: String,
                   name: String,
                   key: String
                   )
