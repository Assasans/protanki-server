package jp.assasans.protanki.server.api

import com.squareup.moshi.Moshi
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.*
import io.ktor.server.plugins.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.jvm.javaio.*
import okio.buffer
import okio.source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MoshiConverter(private val moshi: Moshi) : ContentConverter {
  override suspend fun serialize(contentType: ContentType, charset: Charset, typeInfo: TypeInfo, value: Any): OutgoingContent {
    return TextContent(moshi.adapter<Any>(typeInfo.type.java).toJson(value), contentType.withCharset(charset))
  }

  override suspend fun deserialize(charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel): Any? {
    val stream = withContext(Dispatchers.IO) { content.toInputStream() }
    return moshi.adapter(typeInfo.type.java).fromJson(stream.source().buffer())
  }
}

fun ContentNegotiation.Configuration.moshi(moshi: Moshi = Moshi.Builder().build()) {
  register(ContentType.Application.Json, MoshiConverter(moshi))
}

fun ContentNegotiation.Configuration.moshi(block: Moshi.Builder.() -> Unit) {
  moshi(Moshi.Builder().apply(block).build())
}
