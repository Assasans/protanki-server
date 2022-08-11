package jp.assasans.protanki.server.serialization

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import jp.assasans.protanki.server.client.Screen

class ScreenAdapter {
  @ToJson
  fun toJson(type: Screen): String = type.key

  @FromJson
  fun fromJson(value: String) = Screen.get(value)
}
