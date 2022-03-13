package jp.assasans.protanki.server.commands.handlers

import mu.KotlinLogging
import org.koin.core.component.KoinComponent
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

  @CommandHandler(CommandName.TryMountPreviewItem)
  suspend fun tryMountPreviewItem(socket: UserSocket, item: String) {
    Command(CommandName.MountItem, listOf(item, false.toString())).send(socket)
  }
}
