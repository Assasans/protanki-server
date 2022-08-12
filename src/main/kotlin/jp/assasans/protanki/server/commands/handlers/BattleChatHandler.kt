package jp.assasans.protanki.server.commands.handlers

import kotlin.reflect.jvm.jvmName
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.battles.SendTarget
import jp.assasans.protanki.server.battles.sendTo
import jp.assasans.protanki.server.chat.CommandInvocationSource
import jp.assasans.protanki.server.chat.CommandParseResult
import jp.assasans.protanki.server.chat.IChatCommandRegistry
import jp.assasans.protanki.server.client.BattleChatMessage
import jp.assasans.protanki.server.client.UserSocket
import jp.assasans.protanki.server.client.sendBattleChat
import jp.assasans.protanki.server.client.toJson
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler

class BattleChatHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val chatCommandRegistry by inject<IChatCommandRegistry>()

  @CommandHandler(CommandName.SendBattleChatMessageServer)
  suspend fun sendBattleChatMessageServer(socket: UserSocket, content: String, isTeam: Boolean) {
    val user = socket.user ?: throw Exception("No User")
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val battle = player.battle

    if(content.startsWith("/")) {
      logger.debug { "Parsing message as command: $content" }

      val args = chatCommandRegistry.parseArguments(content.drop(1))
      logger.debug { "Parsed arguments: $args" }

      when(val result = chatCommandRegistry.parseCommand(args)) {
        is CommandParseResult.Success          -> {
          logger.debug { "Parsed command: ${result.parsedCommand.command.name}" }

          try {
            chatCommandRegistry.callCommand(socket, result.parsedCommand, CommandInvocationSource.BattleChat)
          } catch(exception: Exception) {
            logger.error(exception) { "An exception occurred while calling command ${result.parsedCommand.command.name}" }

            val builder = StringBuilder()
            builder.append(exception::class.qualifiedName ?: exception::class.simpleName ?: exception::class.jvmName)
            builder.append(": ")
            builder.append(exception.message ?: exception.localizedMessage)
            builder.append("\n")
            exception.stackTrace.forEach { frame ->
              builder.appendLine("    at $frame")
            }

            socket.sendBattleChat("An exception occurred while calling command ${result.parsedCommand.command.name}\n$builder")
          }
        }

        is CommandParseResult.UnknownCommand   -> {
          logger.debug { "Unknown command: ${result.commandName}" }
          socket.sendBattleChat("Unknown command: ${result.commandName}")
        }

        is CommandParseResult.CommandQuoted    -> {
          logger.debug { "Command name cannot be quoted" }
          socket.sendBattleChat("Command name cannot be quoted")
        }

        is CommandParseResult.TooFewArguments  -> {
          val missingArguments = result.missingArguments.map { argument -> argument.name }.joinToString(", ")

          logger.debug { "Too few arguments for command '${result.command.name}'. Missing values for: $missingArguments" }
          socket.sendBattleChat("Too few arguments for command '${result.command.name}'. Missing values for: $missingArguments")
        }

        is CommandParseResult.TooManyArguments -> {
          logger.debug { "Too many arguments for command '${result.command.name}'. Expected ${result.expected.size}, got: ${result.got.size}" }
          socket.sendBattleChat("Too many arguments for command '${result.command.name}'. Expected ${result.expected.size}, got: ${result.got.size}")
        }
      }
      return
    }

    val message = BattleChatMessage(
      nickname = user.username,
      rank = user.rank.value,
      message = content,
      team = isTeam,
      team_type = player.team
    )

    if(player.isSpectator) {
      if(isTeam) {
        Command(CommandName.SendBattleChatSpectatorTeamMessageClient, "[${user.username}] $content").sendTo(battle, SendTarget.Spectators)
      } else {
        Command(CommandName.SendBattleChatSpectatorMessageClient, content).sendTo(battle)
      }
    } else {
      Command(CommandName.SendBattleChatMessageClient, message.toJson()).sendTo(battle)
    }
  }
}
