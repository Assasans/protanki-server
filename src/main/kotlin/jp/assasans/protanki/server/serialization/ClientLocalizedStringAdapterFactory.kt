package jp.assasans.protanki.server.serialization

import java.lang.reflect.Type
import com.squareup.moshi.*
import jp.assasans.protanki.server.client.SocketLocale
import jp.assasans.protanki.server.utils.ClientLocalizedString

class ClientLocalizedStringAdapterFactory : JsonAdapter.Factory {
  private val mapType = Types.newParameterizedType(
    Map::class.java,
    String::class.java,
    String::class.java
  )

  override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<ClientLocalizedString>? {
    if(type.rawType == ClientLocalizedString::class.java) {
      return object : JsonAdapter<ClientLocalizedString>() {
        override fun fromJson(reader: JsonReader): ClientLocalizedString? {
          return when(val token = reader.peek()) {
            JsonReader.Token.NULL         -> return null
            JsonReader.Token.BEGIN_OBJECT -> ClientLocalizedString(moshi
              .adapter<Map<String, String>>(mapType)
              .fromJson(reader)!!
              .mapKeys { translation -> SocketLocale.get(translation.key)!! })
            else                          -> throw JsonDataException("Unsupported JSON token: $token")
          }
        }

        override fun toJson(writer: JsonWriter, value: ClientLocalizedString?) {
          when(value) {
            null -> writer.nullValue()
            else -> moshi.adapter<Map<String, String>>(mapType).toJson(
              writer,
              value.localized.mapKeys { translation -> translation.key.key }
            )
          }
        }
      }
    }
    return null
  }
}
