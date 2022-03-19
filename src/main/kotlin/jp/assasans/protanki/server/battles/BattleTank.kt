package jp.assasans.protanki.server.battles

import mu.KotlinLogging
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

  private suspend fun killSelf() {
    state = TankState.Dead

    socket.runConnected {
      Command(CommandName.KillLocalTank).send(socket)
    }
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

  suspend fun prepareToSpawn() {
    Command(
      CommandName.PrepareToSpawn,
      listOf(
        id,
        "0.0@0.0@1000.0@0.0"
      )
    ).send(this)
  }

  suspend fun spawn() {
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
          team_type = player.team.key,
          x = 0.0,
          y = 0.0,
          z = 1000.0,
          rot = 0.0,

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
