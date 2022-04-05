package jp.assasans.protanki.server.commands

import kotlin.reflect.*
import com.squareup.moshi.Moshi
import org.koin.java.KoinJavaComponent

data class CommandHandlerDescription(
  val type: KClass<out ICommandHandler>,
  val function: KFunction<*>,
  val name: CommandName,
  var argsBehaviour: ArgsBehaviourType,
  val args: List<KParameter>
)

class CommandArgs(var rawArgs: List<String>) {
  @OptIn(ExperimentalStdlibApi::class)
  companion object {
    fun convert(type: KType, value: String): Any? {
      return when(type.javaType) {
        String::class.java -> value
        Int::class.java    -> value.toInt()
        Double::class.java -> value.toDouble()
        Boolean::class.java -> {
          if(value.equals("false", ignoreCase = true)) return false
          if(value.equals("true", ignoreCase = true)) return true
          throw Exception("Invalid Boolean value: $value")
        }

        else               -> {
          val json = KoinJavaComponent.inject<Moshi>(Moshi::class.java).value
          val adapter = json.adapter<Any>(type.javaType)

          adapter.fromJson(value)
        }

        // else          -> throw Exception("Unsupported parameter: ${parameter.name}: ${parameter.type}")
      }
    }

    inline fun <reified T> convert(value: String): T = convert(typeOf<T>(), value) as T
  }

  val size: Int = rawArgs.size

  fun get(index: Int): String = rawArgs[index]

  inline fun <reified T> getAs(index: Int): T = convert(get(index))
}
