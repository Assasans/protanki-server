package jp.assasans.protanki.server.battles.mode

import jp.assasans.protanki.server.battles.*
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName

abstract class TeamModeHandler(battle: Battle) : BattleModeHandler(battle) {
  var redScore: Int = 0
  var blueScore: Int = 0

  override suspend fun playerJoin(player: BattlePlayer) {
    val players = battle.players.users().filter { battlePlayer -> battlePlayer.team == player.team }.toStatisticsUsers()
    val redPlayers = battle.players.users().filter { battlePlayer -> battlePlayer.team == BattleTeam.Red }.toStatisticsUsers()
    val bluePlayers = battle.players.users().filter { battlePlayer -> battlePlayer.team == BattleTeam.Blue }.toStatisticsUsers()

    Command(
      CommandName.InitTeamStatistics,
      listOf(
        InitTeamStatisticsData(
          reds = redPlayers,
          blues = bluePlayers,
          redScore = 0,
          blueScore = 0
        ).toJson()
      )
    ).send(player)

    battle.players.forEach { battlePlayer ->
      if(battlePlayer == player) return@forEach

      Command(
        CommandName.BattlePlayerJoinTeam,
        listOf(
          BattlePlayerJoinTeamData(
            id = player.user.username,
            team = player.team,
            players = players
          ).toJson()
        )
      ).send(battlePlayer)
    }
  }

  override suspend fun playerLeave(player: BattlePlayer) {
    Command(CommandName.BattlePlayerLeaveTeam, listOf(player.user.username)).sendTo(battle, exclude = player)
  }
}
