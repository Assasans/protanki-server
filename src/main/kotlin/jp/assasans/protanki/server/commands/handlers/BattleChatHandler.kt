package jp.assasans.protanki.server.commands.handlers

import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.battles.BattleTeam
import jp.assasans.protanki.server.battles.sendTo
import jp.assasans.protanki.server.client.BattleChatMessage
import jp.assasans.protanki.server.client.UserSocket
import jp.assasans.protanki.server.client.toJson
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler
import jp.assasans.protanki.server.lobby.chat.ILobbyChatManager

class BattleChatHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val lobbyChatManager by inject<ILobbyChatManager>()

  @CommandHandler(CommandName.SendBattleChatMessageServer)
  suspend fun sendBattleChatMessageServer(socket: UserSocket, content: String, isTeam: Boolean) {
    val user = socket.user ?: throw Exception("No User")
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val battle = player.battle

    val message = BattleChatMessage(
      nickname = user.username,
      rank = user.rank.value,
      message = content,
      team = isTeam,
      team_type = BattleTeam.None // TODO(Assasans)
    )

    Command(CommandName.SendBattleChatMessageClient, listOf(message.toJson())).sendTo(battle)
  }
}
