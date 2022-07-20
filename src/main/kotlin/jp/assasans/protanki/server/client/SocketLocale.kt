package jp.assasans.protanki.server.client

enum class SocketLocale(val key: String, val localizationKey: String) {
  Russian("RU", "ru"),
  English("EN", "en"),
  Portuguese("pt_BR", "pt");

  companion object {
    private val map = values().associateBy(SocketLocale::key)
    private val mapByLocalization = values().associateBy(SocketLocale::localizationKey)

    fun get(key: String) = map[key]
    fun getByLocalization(key: String) = mapByLocalization[key]
  }
}
