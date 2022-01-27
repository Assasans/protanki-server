package jp.assasans.protanki.server.entities

import org.jetbrains.exposed.dao.id.IntIdTable

object Users : IntIdTable() {
  val username = text("username")
  val password = text("password")
  val score = integer("score")
  val crystals = integer("crystals")
}
