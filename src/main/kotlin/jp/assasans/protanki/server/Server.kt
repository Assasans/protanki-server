package jp.assasans.protanki.server

import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.battles.Battle
import jp.assasans.protanki.server.battles.IBattleProcessor
import jp.assasans.protanki.server.battles.map.IMapRegistry
import jp.assasans.protanki.server.battles.map.get
import jp.assasans.protanki.server.commands.ICommandRegistry
import jp.assasans.protanki.server.commands.handlers.*
import jp.assasans.protanki.server.garage.IGarageMarketRegistry

class Server : KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val socketServer by inject<ISocketServer>()
  private val commandRegistry by inject<ICommandRegistry>()
  private val battleProcessor by inject<IBattleProcessor>()
  private val marketRegistry by inject<IGarageMarketRegistry>()
  private val mapRegistry by inject<IMapRegistry>()

  suspend fun run() {
    logger.info { "Server started" }

    mapRegistry.load()
    marketRegistry.load()

    commandRegistry.registerHandlers(SystemHandler::class)
    commandRegistry.registerHandlers(AuthHandler::class)
    commandRegistry.registerHandlers(LobbyHandler::class)
    commandRegistry.registerHandlers(BattleHandler::class)
    commandRegistry.registerHandlers(ShotHandler::class)
    commandRegistry.registerHandlers(GarageHandler::class)
    commandRegistry.registerHandlers(SettingsHandler::class)

    battleProcessor.battles.add(
      Battle(
        id = "493202bf695cc88a",
        title = "ProTanki Server",
        map = mapRegistry.get("map_kungur", ServerMapTheme.SummerDay)
      )
    )

    socketServer.run()
  }
}
