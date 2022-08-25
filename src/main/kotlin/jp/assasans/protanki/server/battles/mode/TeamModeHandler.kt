package jp.assasans.protanki.server.battles.mode

import jp.assasans.protanki.server.battles.*
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName

abstract class TeamModeHandler(battle: Battle) : BattleModeHandler(battle) {
  val teamScores: MutableMap<BattleTeam, Int> = mutableMapOf(
    BattleTeam.Red to 0,
    BattleTeam.Blue to 0
  )

  private val clientTeamScores: MutableMap<BattleTeam, Int> = teamScores.toMutableMap()

  override suspend fun playerJoin(player: BattlePlayer) {
    val players = battle.players.users().filter { battlePlayer -> battlePlayer.team == player.team }.toStatisticsUsers()
    val redPlayers = battle.players.users().filter { battlePlayer -> battlePlayer.team == BattleTeam.Red }.toStatisticsUsers()
    val bluePlayers = battle.players.users().filter { battlePlayer -> battlePlayer.team == BattleTeam.Blue }.toStatisticsUsers()

    Command(
      CommandName.InitTeamStatistics,
      InitTeamStatisticsData(
        reds = redPlayers,
        blues = bluePlayers,
        redScore = 0,
        blueScore = 0
      ).toJson()
    ).send(player)

    if(player.isSpectator) return

    Command(
      CommandName.BattlePlayerJoinTeam,
      BattlePlayerJoinTeamData(
        id = player.user.username,
        team = player.team,
        players = players
      ).toJson()
    ).send(battle.players.exclude(player).ready())
  }

  override suspend fun playerLeave(player: BattlePlayer) {
    if(player.isSpectator) return

    Command(
      CommandName.BattlePlayerLeaveTeam,
      player.user.username
    ).send(battle.players.exclude(player).ready())
  }

  suspend fun updateScores() {
    teamScores
      .filter { (team, score) -> clientTeamScores[team] != score } // Send only changed scores
      .forEach { (team, score) ->
        clientTeamScores[team] = score

        Command(CommandName.ChangeTeamScore, team.key, score.toString()).sendTo(battle)
      }
  }

  override suspend fun dump(builder: StringBuilder) {
    builder.appendLine("    Scores:")
    teamScores.forEach { (team, score) ->
      builder.appendLine("        ${team.name}: $score")
    }
  }
}
