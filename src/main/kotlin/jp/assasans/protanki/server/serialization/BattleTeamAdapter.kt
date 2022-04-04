package jp.assasans.protanki.server.serialization

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import jp.assasans.protanki.server.battles.BattleTeam

class BattleTeamAdapter {
  @ToJson
  fun toJson(type: BattleTeam): String = type.key

  @FromJson
  fun fromJson(value: String) = BattleTeam.get(value)
}
