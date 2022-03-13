package jp.assasans.protanki.server.commands.handlers

import mu.KotlinLogging
import jp.assasans.protanki.server.client.SocketLocale
import jp.assasans.protanki.server.client.UserSocket
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler

class SystemHandler : ICommandHandler {
  private val logger = KotlinLogging.logger { }

  @CommandHandler(CommandName.GetAesData)
  suspend fun getAesData(socket: UserSocket, localeId: String) {
    logger.debug { "Initialized client locale: $localeId" }

    socket.locale = SocketLocale.get(localeId);
  }

  @CommandHandler(CommandName.Error)
  suspend fun error(socket: UserSocket, error: String) {
    logger.warn { "Client-side error occurred: $error" }
  }

  @CommandHandler(CommandName.DependenciesLoaded)
  suspend fun dependenciesLoaded(socket: UserSocket, id: Int) {
    logger.debug { "Loaded dependency $id" }

    socket.markDependencyLoaded(id)
  }
}
