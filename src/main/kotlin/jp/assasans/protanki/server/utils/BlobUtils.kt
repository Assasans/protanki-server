package jp.assasans.protanki.server.utils

class BlobUtils private constructor() {
  companion object {
    fun decode(encoded: String): ByteArray = encoded
      .split(",")
      .map { byte -> byte.toByte() }
      .toByteArray()

    fun encode(bytes: ByteArray): String = bytes.joinToString(",")
  }
}
