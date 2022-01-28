package jp.assasans.protanki.server.client

import org.jetbrains.exposed.sql.ResultRow
import jp.assasans.protanki.server.entities.Users

class User(
  val id: Int,
  var username: String,
  val password: String,
  var score: Int,
  var crystals: Int
) {
  companion object {
    fun fromDatabase(user: ResultRow): User = User(
      id = user[Users.id].value,
      username = user[Users.username],
      password = user[Users.password],
      score = user[Users.score],
      crystals = user[Users.crystals]
    )
  }

  val rank: UserRank
    get() {
      var lastScore = score
      var rank = UserRank.Recruit
      while(lastScore >= rank.score) {
        if(rank == UserRank.Generalissimo) break

        lastScore -= rank.score
        rank = UserRank.get(rank.value) ?: UserRank.Generalissimo
      }
      return rank
    }
}
