package jp.assasans.protanki.server.battles.mode

import jp.assasans.protanki.server.battles.*
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName

class DeathmatchModeHandler(battle: Battle) : BattleModeHandler(battle) {
  companion object {
    fun builder(): BattleModeHandlerBuilder = { battle -> DeathmatchModeHandler(battle) }
  }

  override val mode: BattleMode get() = BattleMode.Deathmatch

  override suspend fun playerJoin(player: BattlePlayer) {
    val players = battle.players.users().toStatisticsUsers()

    Command(
      CommandName.InitDmStatistics,
      listOf(InitDmStatisticsData(users = players).toJson())
    ).send(player)

    battle.players.forEach { battlePlayer ->
      if(battlePlayer == player) return@forEach

      Command(
        CommandName.BattlePlayerJoinDm,
        listOf(
          BattlePlayerJoinDmData(
            id = player.user.username,
            players = players
          ).toJson()
        )
      ).send(battlePlayer)
    }
  }

  override suspend fun playerLeave(player: BattlePlayer) {
    Command(CommandName.BattlePlayerLeaveDm, listOf(player.user.username)).sendTo(battle, exclude = player)
  }

  override suspend fun initModeModel(player: BattlePlayer) {
    Command(CommandName.InitDmModel).send(player)
  }
}
