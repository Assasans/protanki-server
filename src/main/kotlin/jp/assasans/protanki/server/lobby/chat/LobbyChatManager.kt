package jp.assasans.protanki.server.lobby.chat

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.ISocketServer
import jp.assasans.protanki.server.client.ChatMessage
import jp.assasans.protanki.server.client.Screen
import jp.assasans.protanki.server.client.send
import jp.assasans.protanki.server.client.toJson
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.extensions.truncateLastTo

interface ILobbyChatManager {
  val messagesBufferSize: Int
  val messages: MutableList<ChatMessage>

  suspend fun send(message: ChatMessage)
}

class LobbyChatManager : ILobbyChatManager, KoinComponent {
  override val messagesBufferSize: Int = 100 // Original server stores last 70 messages
  override val messages: MutableList<ChatMessage> = mutableListOf()

  private val server by inject<ISocketServer>()

  override suspend fun send(message: ChatMessage) {
    Command(CommandName.SendChatMessageClient, listOf(message.toJson())).let { command ->
      server.players
        .filter { player -> player.screen == Screen.BattleSelect || player.screen == Screen.Garage }
        .forEach { player -> command.send(player) }
    }

    messages.add(message)
    messages.truncateLastTo(messagesBufferSize)
  }
}
