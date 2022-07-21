package jp.assasans.protanki.server.battles.mode

import jp.assasans.protanki.server.ServerMapDominationPoint
import jp.assasans.protanki.server.battles.*
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.math.Vector3

data class PointState(
  val info: ServerMapDominationPoint,
  val progress: Int = 0
)

fun PointState.toDomPoint(): DomPoint {
  return DomPoint(
    id = info.id,
    radius = info.distance,
    x = info.position.x,
    y = info.position.y,
    z = info.position.z,
    score = progress,
    state = "neutral",
    occupated_users = emptyList()
  )
}

class ControlPointsModeHandler(battle: Battle) : TeamModeHandler(battle) {
  companion object {
    fun builder(): BattleModeHandlerBuilder = { battle -> ControlPointsModeHandler(battle) }
  }

  override val mode: BattleMode get() = BattleMode.ControlPoints

  val points = mutableListOf<PointState>()

  init {
    val mapPoints = battle.map.points ?: throw IllegalStateException("Map has no domination points")
    points += mapPoints.map { point -> PointState(point) }
  }

  override suspend fun initModeModel(player: BattlePlayer) {
    Command(
      CommandName.InitDomModel,
      InitDomModelData(
        resources = DomModelResources().toJson(),
        lighting = DomModelLighting().toJson(),
        points = points.map { point -> point.toDomPoint() },
        mine_activation_radius = 5
      ).toJson()
    ).send(player)
  }
}
