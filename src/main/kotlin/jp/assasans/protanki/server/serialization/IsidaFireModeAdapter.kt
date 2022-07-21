package jp.assasans.protanki.server.serialization

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import jp.assasans.protanki.server.client.weapons.isida.IsidaFireMode

class IsidaFireModeAdapter {
  @ToJson
  fun toJson(type: IsidaFireMode): String = type.key

  @FromJson
  fun fromJson(value: String) = IsidaFireMode.get(value)
}
