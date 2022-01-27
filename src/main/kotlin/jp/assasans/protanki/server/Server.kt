package jp.assasans.protanki.server

import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.battles.Battle
import jp.assasans.protanki.server.battles.IBattleProcessor
import jp.assasans.protanki.server.commands.ICommandRegistry
import jp.assasans.protanki.server.commands.handlers.AuthHandler
import jp.assasans.protanki.server.commands.handlers.LobbyHandler

class Server : KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val socketServer by inject<ISocketServer>()
  private val commandRegistry by inject<ICommandRegistry>()
  private val battleProcessor by inject<IBattleProcessor>()
  private val database by inject<IDatabase>()

  init {
    battleProcessor.battles.add(Battle("493202bf695cc88a"))
  }

  suspend fun run() {
    logger.info { "Server started" }

    commandRegistry.registerHandlers(AuthHandler::class)
    commandRegistry.registerHandlers(LobbyHandler::class)

    database.connect()
    socketServer.run()
  }
}
