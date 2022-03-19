package jp.assasans.protanki.server.commands

enum class CommandCategory(val key: String) {
  System("system"),
  Ping("ping"),

  Auth("auth"),
  Registration("registration"),
  PasswordRestore("restore"),

  Lobby("lobby"),
  LobbyChat("lobby_chat"),

  Garage("garage"),

  Battle("battle"),
  BattleChat("chat"),
  BattleSelect("battle_select"),
  BattleCreate("battle_create");

  companion object {
    private val map = values().associateBy(CommandCategory::key)

    fun get(key: String) = map[key]
  }
}
