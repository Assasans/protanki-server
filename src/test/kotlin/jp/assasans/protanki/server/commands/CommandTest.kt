package jp.assasans.protanki.server.commands

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

internal class CommandTest {
  @Test
  fun createClientWithoutArguments() {
    val command = Command(CommandName.AuthAccept)

    assertEquals(CommandSide.Client, command.side)
    assertEquals(CommandCategory.Auth, command.category)
    assertEquals(CommandName.AuthAccept, command.name)
    assertTrue(command.args.isEmpty())
  }

  @Test
  fun createClientWithArguments() {
    val args = listOf("0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15")
    val command = Command(CommandName.SetAesData, *args.toTypedArray())

    assertEquals(CommandSide.Client, command.side)
    assertEquals(CommandCategory.System, command.category)
    assertEquals(CommandName.SetAesData, command.name)
    assertEquals(args, command.args)
  }

  @Test
  fun readServerWithoutArguments() {
    val command = Command()
    command.readFrom("system;get_aes_data".toByteArray())

    assertEquals(CommandSide.Server, command.side)
    assertEquals(CommandCategory.System, command.category)
    assertEquals(CommandName.GetAesData, command.name)
    assertTrue(command.args.isEmpty())
  }

  @Test
  fun readServerWithArguments() {
    val args = listOf("RU")
    val command = Command()
    command.readFrom("system;get_aes_data;${args.joinToString(";")}".toByteArray())

    assertEquals(CommandSide.Server, command.side)
    assertEquals(CommandCategory.System, command.category)
    assertEquals(CommandName.GetAesData, command.name)
    assertEquals(args, command.args)
  }

  @Test
  fun writeClientWithoutArguments() {
    val command = Command(CommandName.Pong)

    assertEquals("battle;pongend~", command.serialize())
  }

  @Test
  fun writeClientWithArguments() {
    val args = listOf("0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15")
    val command = Command(CommandName.SetAesData, *args.toTypedArray())

    assertEquals("system;set_aes_data;${args.joinToString(";")}end~", command.serialize())
  }
}
