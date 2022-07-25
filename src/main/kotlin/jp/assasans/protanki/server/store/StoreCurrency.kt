package jp.assasans.protanki.server.store

enum class StoreCurrency(val key: String, val displayName: String) {
  JPY("jpy", "JPY");

  companion object {
    private val map = values().associateBy(StoreCurrency::key)

    fun get(key: String) = map[key]
  }
}
