package jp.assasans.protanki.server.serialization

import java.lang.reflect.Type
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@JsonQualifier
annotation class SerializeNull {
  companion object {
    val JSON_ADAPTER_FACTORY: JsonAdapter.Factory = object : JsonAdapter.Factory {
      override fun create(type: Type, annotations: Set<Annotation?>, moshi: Moshi): JsonAdapter<*>? {
        val nextAnnotations = Types.nextAnnotations(annotations, SerializeNull::class.java) ?: return null
        return NullIfNullJsonAdapter<Any?>(moshi.nextAdapter(this, type, nextAnnotations))
      }
    }
  }
}
