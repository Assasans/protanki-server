package jp.assasans.protanki.server

import kotlin.reflect.KClass
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import jp.assasans.protanki.server.battles.Battle
import jp.assasans.protanki.server.battles.IBattleProcessor
import jp.assasans.protanki.server.battles.map.IMapRegistry
import jp.assasans.protanki.server.battles.map.get
import jp.assasans.protanki.server.commands.ICommandHandler
import jp.assasans.protanki.server.commands.ICommandRegistry
import jp.assasans.protanki.server.extensions.cast
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

    val reflections = Reflections("jp.assasans.protanki.server")

    reflections.get(Scanners.SubTypes.of(ICommandHandler::class.java).asClass<ICommandHandler>()).forEach { type ->
      val handlerType = type.kotlin.cast<KClass<ICommandHandler>>()

      commandRegistry.registerHandlers(handlerType)
      logger.debug { "Registered command handler: ${handlerType.simpleName}" }
    }

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
