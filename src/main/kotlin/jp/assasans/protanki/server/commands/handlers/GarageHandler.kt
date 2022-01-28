package jp.assasans.protanki.server.commands.handlers

import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.battles.IBattleProcessor
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler

/*
Switch to garage from battle:
-> switch_garage
<- change_layout_state [GARAGE]
<- unload_battle
-> i_exit_from_battle
<- init_messages
* load garage resources *
<- init_garage_items [{"items":[...]}]
-> get_garage_data
<- init_market [{"items":[...]}]
<- end_layout_switch [garage, garage]
<- init_mounted_item [hunter_m0, 227169]
<- init_mounted_item [railgun_m0, 906685]
<- init_mounted_item [green_m0, 966681]
*/

class GarageHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  @CommandHandler(CommandName.SwitchGarage)
  suspend fun switchGarage(socket: UserSocket) {
    logger.debug { "Switch to garage" }

    val player = socket.battlePlayer
    if(player != null) {
      Command(CommandName.UnloadBattle).send(socket)
    }
  }

  @CommandHandler(CommandName.ExitFromBattleNotify)
  suspend fun exitFromBattleNotify(socket: UserSocket) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val battle = player.battle
    battle.players.remove(player)

    Command(
      CommandName.InitMessages,
      listOf(
        InitChatMessagesData(
          messages = listOf(
            ChatMessage(name = "roflanebalo", rang = 4, message = "Ты пидорас")
          )
        ).toJson(),
        InitChatSettings().toJson()
      )
    ).send(socket)

    socket.initBattleList()

    logger.debug { "Select battle ${battle.id} -> ${battle.title}" }

    battle.selectFor(socket)
    battle.showInfoFor(socket)
  }
}
