package jp.assasans.protanki.server.lobby.chat

import kotlin.reflect.jvm.jvmName
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.ISocketServer
import jp.assasans.protanki.server.chat.CommandInvocationSource
import jp.assasans.protanki.server.chat.CommandParseResult
import jp.assasans.protanki.server.chat.IChatCommandRegistry
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.extensions.truncateLastTo

interface ILobbyChatManager {
  val messagesBufferSize: Int
  val messages: MutableList<ChatMessage>

  suspend fun send(socket: UserSocket, message: ChatMessage)
  suspend fun broadcast(message: ChatMessage)
}

class LobbyChatManager : ILobbyChatManager, KoinComponent {
  private val logger = KotlinLogging.logger { }

  override val messagesBufferSize: Int = 100 // Original server stores last 70 messages
  override val messages: MutableList<ChatMessage> = mutableListOf()

  private val server by inject<ISocketServer>()
  private val chatCommandRegistry by inject<IChatCommandRegistry>()

  override suspend fun send(socket: UserSocket, message: ChatMessage) {
    val content = message.message
    if(content.startsWith("/")) {
      logger.debug { "Parsing message as command: $content" }

      val args = chatCommandRegistry.parseArguments(content.drop(1))
      logger.debug { "Parsed arguments: $args" }

      when(val result = chatCommandRegistry.parseCommand(args)) {
        is CommandParseResult.Success          -> {
          logger.debug { "Parsed command: ${result.parsedCommand.command.name}" }

          try {
            chatCommandRegistry.callCommand(socket, result.parsedCommand, CommandInvocationSource.LobbyChat)
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

            socket.sendChat("An exception occurred while calling command ${result.parsedCommand.command.name}\n$builder")
          }
        }

        is CommandParseResult.UnknownCommand   -> {
          logger.debug { "Unknown command: ${result.commandName}" }
          socket.sendChat("Unknown command: ${result.commandName}")
        }

        is CommandParseResult.CommandQuoted    -> {
          logger.debug { "Command name cannot be quoted" }
          socket.sendChat("Command name cannot be quoted")
        }

        is CommandParseResult.TooFewArguments -> {
          val missingArguments = result.missingArguments.map { argument -> argument.name }.joinToString(", ")

          logger.debug { "Too few arguments for command '${result.command.name}'. Missing values for: $missingArguments" }
          socket.sendChat("Too few arguments for command '${result.command.name}'. Missing values for: $missingArguments")
        }

        is CommandParseResult.TooManyArguments -> {
          logger.debug { "Too many arguments for command '${result.command.name}'. Expected ${result.expected.size}, got: ${result.got.size}" }
          socket.sendChat("Too many arguments for command '${result.command.name}'. Expected ${result.expected.size}, got: ${result.got.size}")
        }
      }
      return
    }

    broadcast(message)
  }

  override suspend fun broadcast(message: ChatMessage) {
    Command(CommandName.SendChatMessageClient, message.toJson()).let { command ->
      server.players
        .filter { player -> player.screen == Screen.BattleSelect || player.screen == Screen.Garage }
        .filter { player -> player.active }
        .forEach { player -> command.send(player) }
    }

    messages.add(message)
    messages.truncateLastTo(messagesBufferSize)
  }
}
