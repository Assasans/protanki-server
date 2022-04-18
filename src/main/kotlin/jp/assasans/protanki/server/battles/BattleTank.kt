package jp.assasans.protanki.server.battles

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import mu.KotlinLogging
import jp.assasans.protanki.server.battles.effect.TankEffect
import jp.assasans.protanki.server.battles.mode.CaptureTheFlagModeHandler
import jp.assasans.protanki.server.battles.mode.FlagCarryingState
import jp.assasans.protanki.server.client.SpawnTankData
import jp.assasans.protanki.server.client.UserSocket
import jp.assasans.protanki.server.client.send
import jp.assasans.protanki.server.client.toJson
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.garage.ServerGarageUserItemHull
import jp.assasans.protanki.server.garage.ServerGarageUserItemPaint
import jp.assasans.protanki.server.math.Quaternion
import jp.assasans.protanki.server.math.Vector3
import jp.assasans.protanki.server.toVector

class BattleTank(
  val id: String,
  val player: BattlePlayer,
  val incarnation: Int = 1,
  var state: TankState,
  var position: Vector3,
  var orientation: Quaternion,
  val hull: ServerGarageUserItemHull,
  val weapon: WeaponHandler,
  val coloring: ServerGarageUserItemPaint
) : ITickHandler {
  private val logger = KotlinLogging.logger { }

  val socket: UserSocket
    get() = player.socket

  val battle: Battle
    get() = player.battle

  val coroutineScope = CoroutineScope(player.coroutineScope.coroutineContext + SupervisorJob())

  val effects: MutableList<TankEffect> = mutableListOf()

  suspend fun activate() {
    if(state == TankState.Active) return

    state = TankState.Active

    player.battle.players.users().forEach { player ->
      val tank = player.tank
      if(tank != null && tank != this) {
        Command(CommandName.ActivateTank, listOf(tank.id)).send(socket)
      }
    }

    Command(CommandName.ActivateTank, listOf(id)).sendTo(battle)
  }

  suspend fun deactivate(terminate: Boolean = false) {
    coroutineScope.cancel()

    if(!terminate) {
      effects.forEach { effect ->
        effect.deactivate()
      }
    }
    effects.clear()
  }

  private suspend fun killSelf() {
    deactivate()
    state = TankState.Dead

    (battle.modeHandler as? CaptureTheFlagModeHandler)?.let { handler ->
      val flag = handler.flags[player.team.opposite]
      if(flag is FlagCarryingState && flag.carrier == this) {
        handler.dropFlag(flag.team, this, position)
      }
    }

    Command(CommandName.KillLocalTank).send(socket)
  }

  suspend fun killBy(killer: BattleTank) {
    killSelf()

    Command(
      CommandName.KillTank,
      listOf(
        id,
        TankKillType.ByPlayer.key,
        killer.id
      )
    ).sendTo(battle)
  }

  suspend fun selfDestruct() {
    killSelf()

    Command(
      CommandName.KillTank,
      listOf(
        id,
        TankKillType.SelfDestruct.key,
        id
      )
    ).sendTo(battle)
  }

  fun updateSpawnPosition() {
    val point = battle.map.spawnPoints.random()
    position = point.position.toVector()
    position.z += 200
    orientation.fromEulerAngles(point.position.toVector())

    logger.debug { "Spawn point: $position, $orientation" }
  }

  suspend fun prepareToSpawn() {
    Command(
      CommandName.PrepareToSpawn,
      listOf(
        id,
        "${position.x}@${position.y}@${position.z}@${orientation.toEulerAngles().z}"
      )
    ).send(this)
  }

  suspend fun spawn() {
    state = TankState.SemiActive

    Command(
      CommandName.ChangeHealth,
      listOf(
        id,
        10000.toString()
      )
    ).send(this)

    Command(
      CommandName.SpawnTank,
      listOf(
        SpawnTankData(
          tank_id = id,
          health = 10000,
          incration_id = player.incarnation,
          team_type = player.team,
          x = position.x,
          y = position.y,
          z = position.z,
          rot = orientation.toEulerAngles().z,

          // Hull physics
          speed = hull.modification.physics.speed,
          turn_speed = hull.modification.physics.turnSpeed,
          acceleration = hull.modification.physics.acceleration,
          reverseAcceleration = hull.modification.physics.reverseAcceleration,
          sideAcceleration = hull.modification.physics.sideAcceleration,
          turnAcceleration = hull.modification.physics.turnAcceleration,
          reverseTurnAcceleration = hull.modification.physics.turnSpeed,

          // Weapon physics
          turret_rotation_speed = weapon.item.modification.physics.turretRotationSpeed,
          turretTurnAcceleration = weapon.item.modification.physics.turretTurnAcceleration
        ).toJson()
      )
    ).send(this)
  }
}
