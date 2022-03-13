package jp.assasans.protanki.server.client

enum class Screen(val key: String) {
  BattleSelect("BATTLE_SELECT"),
  Battle("BATTLE"),
  Garage("GARAGE");

  companion object {
    private val map = values().associateBy(Screen::key)

    fun get(key: String) = map[key]
  }
}
