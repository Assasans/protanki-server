package jp.assasans.protanki.server.serialization

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import jp.assasans.protanki.server.ServerMapTheme

class ServerMapThemeAdapter {
  @ToJson
  fun toJson(type: ServerMapTheme): String = type.key

  @FromJson
  fun fromJson(value: String) = ServerMapTheme.get(value)
}
