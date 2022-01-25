package jp.assasans.protanki.server

import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.battles.Battle
import jp.assasans.protanki.server.battles.BattleProcessor

class Server : KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val socketServer by inject<SocketServer>()
  private val battleProcessor by inject<BattleProcessor>()

  init {
    battleProcessor.battles.add(Battle("493202bf695cc88a"))
  }

  suspend fun run() {
    logger.info { "Server started" }

    socketServer.run()
  }
}
