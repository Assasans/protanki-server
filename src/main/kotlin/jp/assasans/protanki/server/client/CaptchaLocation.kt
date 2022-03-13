package jp.assasans.protanki.server.client

enum class CaptchaLocation(val key: String) {
  Login("AUTH"),
  Registration("registration"),

  // Not implemented in client
  ClientStartup("CLIENT_STARTUP"),
  PasswordRestore("RESTORE_PASSWORD_FORM"),
  EmailChangeHash("EMAIL_CHANGE_HASH"),
  AccountSettings("ACCOUNT_SETTINGS_FORM");

  companion object {
    private val map = values().associateBy(CaptchaLocation::key)

    fun get(key: String) = map[key]
  }
}
