package jp.assasans.protanki.server

class EncryptionTransformer {
  private val keys = Array<Int>(9) { index -> index + 1 }
  private var lastKey = 1

  fun decrypt(encrypted: String): String {
    val key = encrypted[0].toString().toInt()

    // println("Decrypting $request")

    return encrypted
      .drop(1)
      .map { value -> Char(value.code - (key + 1)) }
      .joinToString("")
  }

  fun encrypt(plain: String): String {
    var key = (lastKey + 1) % keys.size
    if(key <= 0) key = 1
    lastKey = key

    return key.toString() + plain
      .map { value -> Char(value.code + (key + 1)) }
      .joinToString("")
  }
}
