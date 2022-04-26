package jp.assasans.protanki.server.serialization

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import jp.assasans.protanki.server.SkyboxSide

class SkyboxSideAdapter {
  @ToJson
  fun toJson(type: SkyboxSide): String = type.key

  @FromJson
  fun fromJson(value: String) = SkyboxSide.get(value)
}
