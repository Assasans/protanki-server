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
import jp.assasans.protanki.server.battles.mode.DeathmatchModeHandler
import jp.assasans.protanki.server.chat.*
import jp.assasans.protanki.server.client.sendChat
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
  private val chatCommandRegistry by inject<IChatCommandRegistry>()

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
        map = mapRegistry.get("map_kungur", ServerMapTheme.SummerDay),
        modeHandlerBuilder = DeathmatchModeHandler.builder()
      )
    )

    chatCommandRegistry.apply {
      command("help") {
        description("Show list of commands or help for a specific command")

        argument("command", String::class) {
          description("Command to show help for")
          optional()
        }

        handler {
          val commandName: String? = arguments.getOrNull("command")
          if(commandName == null) {
            socket.sendChat("Available commands: ${commands.joinToString(", ") { command -> command.name }}")
            return@handler
          }

          val command = commands.singleOrNull { command -> command.name == commandName }
          if(command == null) {
            socket.sendChat("Unknown command: $commandName")
            return@handler
          }

          val builder: StringBuilder = StringBuilder()

          builder.append(command.name)
          if(command.description != null) {
            builder.append(" - ${command.description}")
          }
          builder.append("\n")

          if(command.arguments.isNotEmpty()) {
            builder.appendLine("Arguments:")
            command.arguments.forEach { argument ->
              builder.append("    ")
              builder.append("${argument.name}: ${argument.type.simpleName}")
              if(argument.isOptional) builder.append(" (optional)")
              if(argument.description != null) {
                builder.append(" - ${argument.description}")
              }
              builder.appendLine()
            }
          }

          socket.sendChat(builder.toString())
        }
      }

      command("kick") {
        description("Kick a user from the server")

        argument("user", String::class) {
          description("The user to kick")
        }

        handler {
          val username: String = arguments["user"]
          val player = socketServer.players.singleOrNull { socket -> socket.user?.username == username }
          if(player == null) {
            socket.sendChat("User '$username' not found")
            return@handler
          }

          player.deactivate()
          if(player != socket) {
            socket.sendChat("User '$username' has been kicked")
          }
        }
      }
    }

    HibernateUtils.createEntityManager().close() // Initialize database

    socketServer.run()
  }
}
