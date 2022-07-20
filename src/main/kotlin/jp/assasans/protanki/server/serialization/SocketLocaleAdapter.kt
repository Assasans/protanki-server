package jp.assasans.protanki.server.serialization

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import jp.assasans.protanki.server.client.SocketLocale

class SocketLocaleAdapter {
  @ToJson
  fun toJson(type: SocketLocale): String = type.localizationKey

  @FromJson
  fun fromJson(value: String) = SocketLocale.getByLocalization(value)
}
