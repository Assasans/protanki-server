package jp.assasans.protanki.server.store

enum class StorePaymentMethod(val key: String) {
  Freekassa("freekassa"),
  Interkassa("interkassa"),
  PayPal("paypal");

  companion object {
    private val map = values().associateBy(StorePaymentMethod::key)

    fun get(key: String) = map[key]
  }
}
