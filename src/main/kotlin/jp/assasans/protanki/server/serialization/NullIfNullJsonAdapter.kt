package jp.assasans.protanki.server.serialization

import java.io.IOException
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

class NullIfNullJsonAdapter<T>(private val delegate: JsonAdapter<T>) : JsonAdapter<T?>() {
  @Throws(IOException::class)
  override fun fromJson(reader: JsonReader): T? {
    return delegate.fromJson(reader)
  }

  @Throws(IOException::class)
  override fun toJson(writer: JsonWriter, value: T?) {
    if(value == null) {
      val serializeNulls = writer.serializeNulls
      writer.serializeNulls = true
      try {
        delegate.toJson(writer, value)
      } finally {
        writer.serializeNulls = serializeNulls
      }
    } else {
      delegate.toJson(writer, value)
    }
  }

  override fun toString(): String = "$delegate.serializeNulls()"
}
