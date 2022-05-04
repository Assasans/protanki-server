package jp.assasans.protanki.server.utils

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import jp.assasans.protanki.server.ServerIdResource

internal class ResourceUtilsTest {
  @Test
  fun encodeId() {
    assertContentEquals(listOf("0", "0", "0", "0", "0"), ResourceUtils.encodeId(ServerIdResource(0, 0)))
    assertContentEquals(listOf("0", "0", "0", "1", "1"), ResourceUtils.encodeId(ServerIdResource(1, 1)))
    assertContentEquals(listOf("0", "1", "342", "100", "1"), ResourceUtils.encodeId(ServerIdResource(123456, 1)))
    assertContentEquals(listOf("0", "1", "342", "100", "12"), ResourceUtils.encodeId(ServerIdResource(123456, 10)))
    assertContentEquals(listOf("111", "226", "2", "322", "173"), ResourceUtils.encodeId(ServerIdResource(1234567890, 123)))
  }

  @Test
  fun decodeId() {
    assertEquals(ServerIdResource(0, 0), ResourceUtils.decodeId(listOf("0", "0", "0", "0", "0")))
    assertEquals(ServerIdResource(1, 1), ResourceUtils.decodeId(listOf("0", "0", "0", "1", "1")))
    assertEquals(ServerIdResource(123456, 1), ResourceUtils.decodeId(listOf("0", "1", "342", "100", "1")))
    assertEquals(ServerIdResource(123456, 10), ResourceUtils.decodeId(listOf("0", "1", "342", "100", "12")))
    assertEquals(ServerIdResource(1234567890, 123), ResourceUtils.decodeId(listOf("111", "226", "2", "322", "173")))
  }
}
