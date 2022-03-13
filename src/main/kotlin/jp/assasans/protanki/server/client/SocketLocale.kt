package jp.assasans.protanki.server.client

enum class SocketLocale(val key: String) {
  Russian("RU"),
  English("EN"),
  Portuguese("pt_BR");

  companion object {
    private val map = values().associateBy(SocketLocale::key)

    fun get(key: String) = map[key]
  }
}
