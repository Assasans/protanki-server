package jp.assasans.protanki.server.garage

enum class GarageItemType(val key: Int, val categoryKey: String) {
  Weapon(1, "weapon"),
  Hull(2, "armor"),
  Paint(3, "paint"),
  Supply(4, "inventory"),
  Subscription(5, "special"),
  Kit(6, "kit"),
  Present(7, "special");

  companion object {
    private val map = values().associateBy(GarageItemType::key)

    fun get(key: Int) = map[key]
  }
}
