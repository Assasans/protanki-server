package jp.assasans.protanki.server.utils

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

internal class BlobUtilsTest {
  private val asciiData = "Hello, world!".toByteArray()
  private val unicodeData = "こんにちは、世界！".toByteArray()
  private val binaryData = byteArrayOf(0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF)

  @Test
  fun ascii() {
    val encoded = BlobUtils.encode(asciiData)
    assertEquals("72,101,108,108,111,44,32,119,111,114,108,100,33", encoded)

    val decoded = BlobUtils.decode(encoded)
    assertContentEquals(asciiData, decoded)
  }

  @Test
  fun unicode() {
    val encoded = BlobUtils.encode(unicodeData)
    assertEquals("-29,-127,-109,-29,-126,-109,-29,-127,-85,-29,-127,-95,-29,-127,-81,-29,-128,-127,-28,-72,-106,-25,-107,-116,-17,-68,-127", encoded)

    val decoded = BlobUtils.decode(encoded)
    assertContentEquals(unicodeData, decoded)
  }

  @Test
  fun binary() {
    val encoded = BlobUtils.encode(binaryData)
    assertEquals("0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15", encoded)

    val decoded = BlobUtils.decode(encoded)
    assertContentEquals(binaryData, decoded)
  }
}
