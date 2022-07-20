package jp.assasans.protanki.server.serialization

import java.lang.reflect.Type
import com.squareup.moshi.*
import jp.assasans.protanki.server.client.SocketLocale
import jp.assasans.protanki.server.utils.LocalizedString

class LocalizedStringAdapterFactory : JsonAdapter.Factory {
  private val mapType = Types.newParameterizedType(
    Map::class.java,
    SocketLocale::class.java,
    String::class.java
  )

  override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
    if(type.rawType == LocalizedString::class.java) {
      return object : JsonAdapter<LocalizedString>() {
        override fun fromJson(reader: JsonReader): LocalizedString? {
          return when(val token = reader.peek()) {
            JsonReader.Token.NULL -> return null
            JsonReader.Token.STRING -> LocalizedString(mapOf(SocketLocale.English to reader.nextString()))
            JsonReader.Token.BEGIN_OBJECT -> LocalizedString(moshi.adapter<Map<SocketLocale, String>>(mapType).fromJson(reader)!!)
            else -> throw JsonDataException("Unsupported JSON token: $token")
          }
        }

        override fun toJson(writer: JsonWriter, value: LocalizedString?) {
          when(value) {
            null -> writer.nullValue()
            else -> moshi.adapter<Map<SocketLocale, String>>(mapType).toJson(writer, value.localized)
          }
        }
      }
    }
    return null
  }
}
