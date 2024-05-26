package jp.assasans.protanki.server.battles

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.floor
import jp.assasans.protanki.server.ISocketServer
import jp.assasans.protanki.server.battles.effect.TankEffect
import jp.assasans.protanki.server.battles.mode.CaptureTheFlagModeHandler
import jp.assasans.protanki.server.battles.mode.FlagCarryingState
import jp.assasans.protanki.server.battles.mode.TeamModeHandler
import jp.assasans.protanki.server.battles.weapons.WeaponHandler
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.garage.ServerGarageUserItemHull
import jp.assasans.protanki.server.garage.ServerGarageUserItemPaint
import jp.assasans.protanki.server.math.Quaternion
import jp.assasans.protanki.server.math.Vector3
import jp.assasans.protanki.server.math.distanceTo
import jp.assasans.protanki.server.quests.KillEnemyQuest
import jp.assasans.protanki.server.quests.questOf
import jp.assasans.protanki.server.toVector

object TankConstants {
  const val MAX_HEALTH: Double = 10000.0
}

class BattleTank(
  val id: String,
  val player: BattlePlayer,
  val incarnation: Int = 1,
  var state: TankState,
  var position: Vector3,
  var orientation: Quaternion,
  val hull: ServerGarageUserItemHull,
  val weapon: WeaponHandler,
  val coloring: ServerGarageUserItemPaint,
  var maxHealth: Double = 4000.0, // TODO(Assasans): Load from config
  var health: Double = 4000.0
) : KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val server: ISocketServer by inject()

  val socket: UserSocket
    get() = player.socket

  val battle: Battle
    get() = player.battle

  val coroutineScope = CoroutineScope(player.coroutineScope.coroutineContext + SupervisorJob())

  val effects: MutableList<TankEffect> = mutableListOf()

  var selfDestructing: Boolean = false

  val clientHealth: Int
    get() = floor((health / maxHealth) * TankConstants.MAX_HEALTH).toInt()

  suspend fun activate() {
    if(state == TankState.Active) return

    state = TankState.Active

    player.battle.players.users().forEach { player ->
      val tank = player.tank
      if(tank != null && tank != this) {
        Command(CommandName.ActivateTank, tank.id).send(socket)
      }
    }

    Command(CommandName.ActivateTank, id).sendTo(battle)
  }

  suspend fun deactivate(terminate: Boolean = false) {
    coroutineScope.cancel()

    if(!terminate) {
      effects.forEach { effect ->
        effect.deactivate()
      }
    }
    effects.clear()

    if(terminate || battle.properties[BattleProperty.DeactivateMinesOnDeath]) {
      battle.mineProcessor.deactivateAll(player)
    }
  }

  private suspend fun killSelf() {
    deactivate()
    state = TankState.Dead

    player.deaths++
    player.updateStats()

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
      id,
      TankKillType.ByPlayer.key,
      killer.id
    ).sendTo(battle)

    killer.player.kills++
    killer.player.updateStats()

    Command(
      CommandName.UpdatePlayerKills,
      battle.id,
      killer.player.user.username,
      killer.player.kills.toString()
    ).let { command ->
      server.players
        .filter { player -> player.screen == Screen.BattleSelect }
        .filter { player -> player.active }
        .forEach { player -> command.send(player) }
    }

    player.user.questOf<KillEnemyQuest> { quest -> quest.mode == null || quest.mode == battle.modeHandler.mode }?.let { quest ->
      quest.current++
      socket.updateQuests()
      quest.updateProgress()
    }
  }

  suspend fun selfDestruct(silent: Boolean = false) {
    killSelf()

    if(silent) {
      Command(CommandName.KillTankSilent, id).sendTo(battle)
    } else {
      Command(
        CommandName.KillTank,
        id,
        TankKillType.SelfDestruct.key,
        id
      ).sendTo(battle)
    }
  }

  fun updateSpawnPosition() {
    // TODO(Assasans): Special handling for CP: https://web.archive.org/web/20160310101712/http://ru.tankiwiki.com/%D0%9A%D0%BE%D0%BD%D1%82%D1%80%D0%BE%D0%BB%D1%8C_%D1%82%D0%BE%D1%87%D0%B5%D0%BA
    val point = battle.map.spawnPoints
      .filter { point -> point.mode == null || point.mode == battle.modeHandler.mode }
      .filter { point -> point.team == null || point.team == player.team }
      .random()
    position = point.position.toVector()
    position.z += 200
    orientation.fromEulerAngles(point.position.toVector())

    logger.debug { "Spawn point: $position, $orientation" }
  }

  suspend fun prepareToSpawn() {
    Command(
      CommandName.PrepareToSpawn,
      id,
      "${position.x}@${position.y}@${position.z}@${orientation.toEulerAngles().z}"
    ).send(this)
  }

  suspend fun initSelf() {
    Command(
      CommandName.InitTank,
      getInitTank().toJson()
    ).send(battle.players.ready())
  }

  suspend fun spawn() {
    state = TankState.SemiActive

    // TODO(Assasans): Add spawn event?
    if(player.equipmentChanged) {
      player.equipmentChanged = false
      player.changeEquipment()
    }

    updateHealth()

    Command(
      CommandName.SpawnTank,
      getSpawnTank().toJson()
    ).send(battle.players.ready())
  }

  suspend fun updateHealth() {
    logger.debug { "Updating health for tank $id (player: ${player.user.username}): $health HP / $maxHealth HP -> $clientHealth" }

    Command(
      CommandName.ChangeHealth,
      id,
      clientHealth.toString()
    ).apply {
      send(this@BattleTank)
      sendTo(battle, SendTarget.Spectators)
      if(battle.modeHandler is TeamModeHandler) {
        battle.players
          .filter { player -> player.team == this@BattleTank.player.team }
          .filter { player -> player != this@BattleTank.player }
          .forEach { player -> send(player.socket) }
      }
    }
  }
}

fun BattleTank.distanceTo(another: BattleTank): Double {
  return position.distanceTo(another.position)
}

fun BattleTank.getInitTank() = InitTankData(
  battleId = battle.id,
  hull_id = hull.mountName,
  turret_id = weapon.item.mountName,
  colormap_id = coloring.marketItem.coloring,
  hullResource = hull.modification.object3ds,
  turretResource = weapon.item.modification.object3ds,
  partsObject = TankSoundsData().toJson(),
  tank_id = id,
  nickname = player.user.username,
  team_type = player.team,
  state = state.tankInitKey,
  health = clientHealth,

  // Hull physics
  maxSpeed = hull.modification.physics.speed,
  maxTurnSpeed = hull.modification.physics.turnSpeed,
  acceleration = hull.modification.physics.acceleration,
  reverseAcceleration = hull.modification.physics.reverseAcceleration,
  sideAcceleration = hull.modification.physics.sideAcceleration,
  turnAcceleration = hull.modification.physics.turnAcceleration,
  reverseTurnAcceleration = hull.modification.physics.reverseTurnAcceleration,
  dampingCoeff = hull.modification.physics.damping,
  mass = hull.modification.physics.mass,
  power = hull.modification.physics.power,

  // Weapon physics
  turret_turn_speed = weapon.item.modification.physics.turretRotationSpeed,
  turretTurnAcceleration = weapon.item.modification.physics.turretTurnAcceleration,
  kickback = weapon.item.modification.physics.kickback,
  impact_force = weapon.item.modification.physics.impactForce,

  // Weapon visual
  sfxData = (weapon.item.modification.visual ?: weapon.item.marketItem.modifications[0]!!.visual)!!.toJson() // TODO(Assasans)
)

fun BattleTank.getSpawnTank() = SpawnTankData(
  tank_id = id,
  health = clientHealth,
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
  reverseTurnAcceleration = hull.modification.physics.reverseTurnAcceleration,

  // Weapon physics
  turret_rotation_speed = weapon.item.modification.physics.turretRotationSpeed,
  turretTurnAcceleration = weapon.item.modification.physics.turretTurnAcceleration
)
