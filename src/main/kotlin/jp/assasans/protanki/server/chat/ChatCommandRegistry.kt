package jp.assasans.protanki.server.chat

import kotlin.reflect.KClass
import org.koin.core.component.KoinComponent
import jp.assasans.protanki.server.client.UserSocket
import jp.assasans.protanki.server.client.sendBattleChat
import jp.assasans.protanki.server.client.sendChat

interface IChatCommandRegistry {
  val commands: MutableList<Command>

  fun parseArguments(input: String): ParsedArguments
  fun parseCommand(parsedArguments: ParsedArguments): CommandParseResult
  suspend fun callCommand(socket: UserSocket, parsed: ParsedCommand, source: CommandInvocationSource)
}

abstract class CommandParseResult(val inputArguments: ParsedArguments) {
  class Success(inputArguments: ParsedArguments, val parsedCommand: ParsedCommand) : CommandParseResult(inputArguments)
  class UnknownCommand(inputArguments: ParsedArguments, val commandName: String) : CommandParseResult(inputArguments)
  class CommandQuoted(inputArguments: ParsedArguments, val commandName: String) : CommandParseResult(inputArguments)

  class TooFewArguments(
    inputArguments: ParsedArguments,
    val command: Command,
    val missingArguments: List<CommandArgument<*>>
  ) : CommandParseResult(inputArguments)

  class TooManyArguments(
    inputArguments: ParsedArguments,
    val command: Command,
    val expected: List<CommandArgument<*>>,
    val got: List<ParsedArgument>
  ) : CommandParseResult(inputArguments)
}

class ChatCommandRegistry : IChatCommandRegistry, KoinComponent {
  override val commands: MutableList<Command> = mutableListOf()

  enum class ArgumentToken {
    Start,
    Quote,
    Escape,
    EscapeQuote
  }

  override fun parseArguments(input: String): ParsedArguments {
    val arguments = mutableListOf<ParsedArgument>()

    var token = ArgumentToken.Start
    var current = ""
    var currentQuoted = false

    for(char in input) {
      when(token) {
        ArgumentToken.Start       -> {
          when(char) {
            '\\' -> token = ArgumentToken.Escape

            '"'  -> {
              token = ArgumentToken.Quote
              currentQuoted = true
            }

            ' '  -> if(current.isNotEmpty()) {
              arguments.add(ParsedArgument(current, currentQuoted))
              current = ""
              currentQuoted = false
            }
            else -> current += char
          }
        }

        ArgumentToken.Escape      -> {
          current += char
          token = ArgumentToken.Start
        }

        ArgumentToken.Quote       -> {
          when(char) {
            '\\' -> token = ArgumentToken.EscapeQuote
            '"'  -> token = ArgumentToken.Start
            else -> current += char
          }
        }

        ArgumentToken.EscapeQuote -> {
          current += char
          token = ArgumentToken.Quote
        }
      }
    }
    if(current.isNotEmpty()) arguments.add(ParsedArgument(current, currentQuoted))

    return ParsedArguments(input, arguments)
  }

  override fun parseCommand(parsedArguments: ParsedArguments): CommandParseResult {
    val commandName = parsedArguments.arguments[0].value
    if(parsedArguments.arguments[0].isQuoted) return CommandParseResult.CommandQuoted(parsedArguments, commandName)
    var command = commands.find { command ->
      if(command.name.equals(commandName, ignoreCase = true)) return@find true
      if(command.aliases.any { alias -> alias.equals(commandName, ignoreCase = true) }) return@find true
      return@find false
    } ?: return CommandParseResult.UnknownCommand(parsedArguments, commandName)

    val arguments = mutableMapOf<CommandArgument<*>, Any?>()
    var argumentsAsCommands = 1
    var canParseSubcommands = true
    for(i in 1 until parsedArguments.arguments.size) {
      val argument = parsedArguments.arguments[i]
      if(canParseSubcommands && !argument.isQuoted) {
        if(command.subcommands.isNotEmpty()) {
          val subcommand = command.subcommands.find { subcommand ->
            if(subcommand.name.equals(argument.value, ignoreCase = true)) return@find true
            if(subcommand.aliases.any { alias -> alias.equals(argument.value, ignoreCase = true) }) return@find true
            return@find false
          }
          if(subcommand != null) {
            command = subcommand
            argumentsAsCommands++
            continue
          }
        }
      }
      canParseSubcommands = false

      val argumentIndex = i - argumentsAsCommands
      if(argumentIndex >= command.arguments.size) {
        return CommandParseResult.TooManyArguments(
          parsedArguments,
          command,
          command.arguments,
          parsedArguments.arguments.drop(argumentsAsCommands)
        )
      }
      val argumentDefinition = command.arguments[argumentIndex]
      arguments[argumentDefinition] = argument.value
    }

    val missingArguments = mutableListOf<CommandArgument<*>>()
    command.arguments
      .filter { it !in arguments }
      .forEach { argument ->
        val defaultValue = argument.defaultValue
        if(argument.isOptional) {
          arguments[argument] = defaultValue
        } else {
          missingArguments.add(argument)
        }
      }
    if(missingArguments.isNotEmpty()) return CommandParseResult.TooFewArguments(parsedArguments, command, missingArguments)

    return CommandParseResult.Success(parsedArguments, ParsedCommand(command, ParsedCommandArguments(arguments)))
  }

  override suspend fun callCommand(socket: UserSocket, parsed: ParsedCommand, source: CommandInvocationSource) {
    val handler = parsed.command.handler ?: throw IllegalArgumentException("Command ${parsed.command.name} has no handler")
    val context = CommandContext(socket, parsed.command, parsed.arguments, source)
    context.apply { handler() }
  }
}

data class ParsedArguments(val input: String, val arguments: List<ParsedArgument>)

data class ParsedArgument(
  val value: String,
  val isQuoted: Boolean
)

fun IChatCommandRegistry.command(name: String, builder: Command.() -> Unit) {
  commands += Command(name).apply(builder)
}

class ParsedCommandArguments(
  val arguments: Map<CommandArgument<*>, *>
) {
  operator fun <T : Any> get(argument: CommandArgument<T>): T = arguments[argument] as T
  operator fun <T : Any> get(name: String): T = getOrNull(name) ?: throw IllegalArgumentException("No argument with name $name")

  fun <T : Any> getOrNull(argument: CommandArgument<T>): T? {
    val value = arguments.getOrDefault(argument, null) ?: return null
    return value as T
  }

  fun <T : Any> getOrNull(name: String): T? {
    val argument = arguments
      .filter { (argument, _) -> argument.name == name }
      .toList()
      .single()
    argument.second ?: return null
    return argument.second as T
  }
}

class ParsedCommand(
  val command: Command,
  val arguments: ParsedCommandArguments
)

enum class CommandInvocationSource {
  LobbyChat,
  BattleChat
}

class CommandContext(
  val socket: UserSocket,
  val command: Command,
  val arguments: ParsedCommandArguments,
  val source: CommandInvocationSource
) {
  suspend fun reply(message: String, warning: Boolean = false) {
    when(source) {
      CommandInvocationSource.LobbyChat  -> socket.sendChat(message, warning)
      CommandInvocationSource.BattleChat -> socket.sendBattleChat(message)
    }
  }
}

class Command(val name: String) {
  var description: String? = null

  val aliases: MutableList<String> = mutableListOf()
  val subcommands: MutableList<Command> = mutableListOf()
  val arguments: MutableList<CommandArgument<*>> = mutableListOf()

  var handler: (suspend CommandContext.() -> Unit)? = null
}

fun Command.description(description: String) {
  this.description = description
}

fun Command.alias(name: String) {
  this.aliases += name
}

fun Command.subcommand(name: String, builder: Command.() -> Unit) {
  subcommands += Command(name).apply(builder)
}

fun <T : Any> Command.argument(name: String, type: KClass<T>, builder: CommandArgument<T>.() -> Unit) {
  arguments += CommandArgument<T>(name, type).apply(builder)
}

fun Command.handler(handler: suspend CommandContext.() -> Unit) {
  this.handler = handler
}

class CommandArgument<T : Any>(val name: String, val type: KClass<T>) {
  var description: String? = null
  var defaultValue: T? = null

  var isOptional: Boolean = false
}

fun <T : Any> CommandArgument<T>.description(description: String) {
  this.description = description
}

fun <T : Any> CommandArgument<T>.defaultValue(defaultValue: T) {
  this.defaultValue = defaultValue
  this.isOptional = true
}

fun <T : Any> CommandArgument<T>.optional(isOptional: Boolean = true) {
  this.isOptional = isOptional
}
