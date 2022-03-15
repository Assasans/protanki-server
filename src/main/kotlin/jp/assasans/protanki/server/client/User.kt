package jp.assasans.protanki.server.client

import jp.assasans.protanki.server.garage.IServerGarageUserItem
import jp.assasans.protanki.server.garage.ServerGarageUserItemHull
import jp.assasans.protanki.server.garage.ServerGarageUserItemPaint
import jp.assasans.protanki.server.garage.ServerGarageUserItemWeapon

data class UserEquipment(
  val hull: ServerGarageUserItemHull,
  val weapon: ServerGarageUserItemWeapon,
  val paint: ServerGarageUserItemPaint
)

class User(
  val id: Int,
  var username: String,
  val password: String,
  var score: Int,
  var crystals: Int,

  val items: List<IServerGarageUserItem>,
  var equipment: UserEquipment
) {
  companion object {
    // fun fromDatabase(user: ResultRow): User = User(
    //   id = user[Users.id].value,
    //   username = user[Users.username],
    //   password = user[Users.password],
    //   score = user[Users.score],
    //   crystals = user[Users.crystals],
    //
    //   items = mutableListOf()
    // )
  }

  // TODO(Assasans): Rewrite
  val rank: UserRank
    get() {
      // var lastScore = score
      // var rank = UserRank.Recruit
      // while(lastScore >= rank.score) {
      //   if(rank == UserRank.Generalissimo) break
      //
      //   lastScore -= rank.score
      //   rank = UserRank.get(rank.value + 1) ?: UserRank.Generalissimo
      // }
      //
      // return rank

      var rank = UserRank.Recruit
      while(score >= rank.score) {
        if(rank == UserRank.Generalissimo) break
        rank = UserRank.get(rank.value + 1) ?: UserRank.Generalissimo
      }

      return rank
    }

  val currentRankScore: Int
    get() {
      // var lastScore = score
      // var rank = UserRank.Recruit
      // while(lastScore >= rank.score) {
      //   if(rank == UserRank.Generalissimo) break
      //
      //   lastScore -= rank.score
      //   rank = UserRank.get(rank.value + 1) ?: UserRank.Generalissimo
      // }
      // return lastScore

      return score - rank.score
    }

  val nextRankScore: Int
    get() {
      val nextRank = UserRank.get(rank.value + 1)
      if(nextRank != null) return nextRank.score
      return 0
    }
}
