package jp.assasans.protanki.server.serialization

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import jp.assasans.protanki.server.BonusType

class BonusTypeMapAdapter {
  @ToJson
  fun toJson(type: BonusType): String = type.mapKey

  @FromJson
  fun fromJson(value: String) = BonusType.get(value)
}
