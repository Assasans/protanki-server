package jp.assasans.protanki.server.utils

import jp.assasans.protanki.server.client.SocketLocale

class ClientLocalizedString(
  val localized: Map<SocketLocale, String>
) {
  val default: String
    get() = localized[SocketLocale.English] ?: throw IllegalStateException("No default localized string")

  fun get(locale: SocketLocale): String = localized[locale] ?: default
}

fun Map<SocketLocale, String>.toClientLocalizedString() = ClientLocalizedString(this)
