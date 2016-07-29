package com.leagueprojecto.api.domain

case class Champion(
                   tags: Seq[String],
                   id: Int,
                   title: String,
                   name: String,
                   key: String
                   )
