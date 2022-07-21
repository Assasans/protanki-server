 package jp.assasans.protanki.server.commands.handlers

import mu.KotlinLogging
import jp.assasans.protanki.server.client.ShowSettingsData
import jp.assasans.protanki.server.client.UserSocket
import jp.assasans.protanki.server.client.send
import jp.assasans.protanki.server.client.toJson
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler

class SettingsHandler : ICommandHandler {
  private val logger = KotlinLogging.logger { }

  @CommandHandler(CommandName.ShowSettings)
  suspend fun showSettings(socket: UserSocket) {
    Command(CommandName.ClientShowSettings, ShowSettingsData().toJson()).send(socket)
  }

  @CommandHandler(CommandName.CheckPasswordIsSet)
  suspend fun checkPasswordIsSet(socket: UserSocket) {
    Command(CommandName.PasswordIsSet).send(socket)
  }
}
