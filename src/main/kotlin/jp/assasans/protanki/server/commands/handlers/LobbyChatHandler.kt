package jp.assasans.protanki.server.commands.handlers

import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler
import jp.assasans.protanki.server.lobby.chat.ILobbyChatManager

class LobbyChatHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val lobbyChatManager by inject<ILobbyChatManager>()

  @CommandHandler(CommandName.SendChatMessageServer)
  suspend fun sendChatMessageServer(socket: UserSocket, nameTo: String, content: String) {
    val user = socket.user ?: throw Exception("No User")

    val message = ChatMessage(
      name = user.username,
      rang = user.rank.value,
      message = content,
      nameTo = nameTo,
      addressed = nameTo.isNotEmpty()
    )

    lobbyChatManager.send(socket, message)
  }
}
