package jp.assasans.protanki.server.serialization

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import jp.assasans.protanki.server.battles.BattleMode

class BattleModeAdapter {
  @ToJson
  fun toJson(type: BattleMode): String = type.key

  @FromJson
  fun fromJson(value: String) = BattleMode.get(value)
}
