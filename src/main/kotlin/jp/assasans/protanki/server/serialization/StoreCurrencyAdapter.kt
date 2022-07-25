package jp.assasans.protanki.server.serialization

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import jp.assasans.protanki.server.store.StoreCurrency

class StoreCurrencyAdapter {
  @ToJson
  fun toJson(type: StoreCurrency): String = type.key

  @FromJson
  fun fromJson(value: String) = StoreCurrency.get(value)
}
