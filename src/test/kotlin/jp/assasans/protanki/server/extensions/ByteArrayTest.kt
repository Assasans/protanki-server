package jp.assasans.protanki.server.extensions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

internal class ByteArrayTest {
  @Test
  fun indexOfSequence_earlyReturns() {
    assertThrows<IllegalArgumentException> { byteArrayOf(0).indexOfSequence(byteArrayOf(0), startFrom = -1) }
    assertEquals(-1, byteArrayOf().indexOfSequence(byteArrayOf(0)))
    assertEquals(0, byteArrayOf(0).indexOfSequence(byteArrayOf()))
    assertEquals(-1, byteArrayOf(0).indexOfSequence(byteArrayOf(0), startFrom = 1))
  }

  @Test
  fun indexOfSequence_basic() {
    assertEquals(3, byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10).indexOfSequence(byteArrayOf(3)))
    assertEquals(10, byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10).indexOfSequence(byteArrayOf(10)))
    assertEquals(5, byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10).indexOfSequence(byteArrayOf(5, 6, 7)))
    assertEquals(7, byteArrayOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0).indexOfSequence(byteArrayOf(3, 2, 1)))

    assertEquals(-1, byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10).indexOfSequence(byteArrayOf(2, 1, 1, 2)))
    assertEquals(-1, byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10).indexOfSequence(byteArrayOf(11)))
  }

  @Test
  fun indexOfSequence_repeatedFirstValue() {
    assertEquals(6, byteArrayOf(0, 1, 2, 3, 4, 21, 21, 12, 20, 19).indexOfSequence(byteArrayOf(21, 12, 20, 19)))
    assertEquals(1, byteArrayOf(21, 21, 12, 20, 19, 6, 7, 8, 9, 10).indexOfSequence(byteArrayOf(21, 12, 20, 19)))

    assertEquals(7, byteArrayOf(0, 1, 2, 3, 4, 21, 21, 21, 12, 20, 19).indexOfSequence(byteArrayOf(21, 12, 20, 19)))
    assertEquals(2, byteArrayOf(21, 21, 21, 12, 20, 19, 6, 7, 8, 9, 10).indexOfSequence(byteArrayOf(21, 12, 20, 19)))
  }

  @Test
  fun indexOfSequence_startFrom() {
    assertEquals(1, byteArrayOf(0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3).indexOfSequence(byteArrayOf(1, 2), startFrom = 1))
    assertEquals(5, byteArrayOf(0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3).indexOfSequence(byteArrayOf(1, 2), startFrom = 4))
    assertEquals(9, byteArrayOf(0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3).indexOfSequence(byteArrayOf(1, 2), startFrom = 9))

    assertEquals(-1, byteArrayOf(0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0, 0, 0).indexOfSequence(byteArrayOf(1, 2), startFrom = 10))
    assertEquals(-1, byteArrayOf(0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0, 0, 0).indexOfSequence(byteArrayOf(12), startFrom = 10))
  }
}
