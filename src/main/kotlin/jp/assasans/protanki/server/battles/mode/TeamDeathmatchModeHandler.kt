package jp.assasans.protanki.server.battles.mode

import jp.assasans.protanki.server.battles.*
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName

class TeamDeathmatchModeHandler(battle: Battle) : TeamModeHandler(battle) {
  companion object {
    fun builder(): BattleModeHandlerBuilder = { battle -> TeamDeathmatchModeHandler(battle) }
  }

  override val mode: BattleMode get() = BattleMode.TeamDeathmatch

  override suspend fun initModeModel(player: BattlePlayer) {
    Command(CommandName.InitTdmModel).send(player)
  }
}
