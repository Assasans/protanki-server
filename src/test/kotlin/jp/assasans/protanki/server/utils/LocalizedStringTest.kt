package jp.assasans.protanki.server.utils

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import jp.assasans.protanki.server.client.SocketLocale

internal class LocalizedStringTest {
  @Test
  fun localized() {
    val strings = mapOf(
      SocketLocale.English to "Hello World!",
      SocketLocale.Russian to "Привет, мир!"
    )
    val string = LocalizedString(strings)

    assertEquals(strings, string.localized)
    assertEquals(strings[SocketLocale.English], string.get(SocketLocale.English))
    assertEquals(strings[SocketLocale.Russian], string.get(SocketLocale.Russian))
  }

  @Test
  fun localizedDefault() {
    val strings = mapOf(SocketLocale.English to "Hello World!")
    val string = LocalizedString(strings)

    assertEquals(strings[SocketLocale.English], string.default)
    assertEquals(string.default, string.get(SocketLocale.English))
    assertEquals(string.default, string.get(SocketLocale.Russian))
    assertEquals(string.default, string.get(SocketLocale.Portuguese))
  }

  @Test
  fun localizedEmpty() {
    val string = LocalizedString(emptyMap<SocketLocale, String>())

    assertTrue(string.localized.isEmpty())
    assertFailsWith<IllegalStateException> { string.default }
    assertFailsWith<IllegalStateException> { string.get(SocketLocale.English) }
    assertFailsWith<IllegalStateException> { string.get(SocketLocale.Russian) }
    assertFailsWith<IllegalStateException> { string.get(SocketLocale.Portuguese) }
  }
}
