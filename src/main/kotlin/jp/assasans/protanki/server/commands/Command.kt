package jp.assasans.protanki.server.commands

import java.io.OutputStream
import mu.KotlinLogging
import jp.assasans.protanki.server.exceptions.UnknownCommandCategoryException
import jp.assasans.protanki.server.exceptions.UnknownCommandException

class Command {
  private val logger = KotlinLogging.logger { }

  companion object {
    const val Delimiter = "end~"
  }

  lateinit var name: CommandName
    private set

  lateinit var args: MutableList<String>
    private set

  val category: CommandCategory
    get() = name.category

  val side: CommandSide
    get() = name.side

  constructor() {}
  constructor(name: CommandName, args: MutableList<String>? = null) : this() {
    this.name = name
    this.args = args ?: mutableListOf()
  }

  fun serialize(): String {
    val builder = StringBuilder()
    builder.append(category.key)
    builder.append(";")
    builder.append(name.key)
    if(args.isNotEmpty()) {
      builder.append(";")
      builder.append(args.joinToString(";"))
    }
    builder.append(Delimiter)

    // println("Write command: $builder")

    return builder.toString()
  }

  fun readFrom(reader: ByteArray) {
    val data = String(reader)
    val args = data.split(";")
    val categoryRaw = args[0]
    val nameRaw = args[1]

    val category = CommandCategory.get(categoryRaw)
    val name = CommandName.get(nameRaw)

    category ?: throw UnknownCommandCategoryException(categoryRaw, "Unknown command category: $categoryRaw")
    name ?: throw UnknownCommandException(categoryRaw, nameRaw, "Unknown command name: $categoryRaw::$nameRaw")

    if(name.category != category) throw Exception("Command category mismatch. Expected: ${name.category}, got: $category")

    this.name = name
    this.args = args.drop(2).toMutableList()
  }
}
