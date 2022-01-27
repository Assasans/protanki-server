package jp.assasans.protanki.server.commands

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

data class CommandHandlerDescription(
  val type: KClass<*>,
  val function: KFunction<*>,
  val name: CommandName,
  val args: List<KParameter>
)
