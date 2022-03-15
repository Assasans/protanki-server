package jp.assasans.protanki.server

import com.squareup.moshi.Moshi
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.battles.Battle
import jp.assasans.protanki.server.battles.BattleMap
import jp.assasans.protanki.server.battles.IBattleProcessor
import jp.assasans.protanki.server.commands.ICommandRegistry
import jp.assasans.protanki.server.commands.handlers.*
import jp.assasans.protanki.server.garage.IGarageMarketRegistry

class Server : KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val socketServer by inject<ISocketServer>()
  private val commandRegistry by inject<ICommandRegistry>()
  private val battleProcessor by inject<IBattleProcessor>()
  private val database by inject<IDatabase>()
  private val marketRegistry by inject<IGarageMarketRegistry>()

  init {
    battleProcessor.battles.add(
      Battle(
        id = "493202bf695cc88a",
        title = "ProTanki Server",
        map = BattleMap(
          id = 663288,
          name = "map_sandbox",
          preview = 618467
        )
      )
    )
  }

  suspend fun run() {
    logger.info { "Server started" }

    marketRegistry.load()

    commandRegistry.registerHandlers(SystemHandler::class)
    commandRegistry.registerHandlers(AuthHandler::class)
    commandRegistry.registerHandlers(LobbyHandler::class)
    commandRegistry.registerHandlers(BattleHandler::class)
    commandRegistry.registerHandlers(ShotHandler::class)
    commandRegistry.registerHandlers(GarageHandler::class)

    socketServer.run()
  }
}
