package jp.assasans.protanki.server.client

import com.squareup.moshi.Json
import jp.assasans.protanki.server.battles.BattleTeam
import jp.assasans.protanki.server.garage.HullPhysics
import jp.assasans.protanki.server.garage.WeaponPhysics

data class GuiUserData(
  @Json val nickname: String,
  @Json val rank: Int,
  @Json val teamType: BattleTeam
)

data class InitGuiModelData(
  @Json val name: String,
  @Json val fund: Int,
  @Json val scoreLimit: Int,
  @Json val timeLimit: Int,
  @Json val currTime: Int,
  @Json val score_red: Int = 0,
  @Json val score_blue: Int = 0,
  @Json val team: Boolean, // Is team battle
  @Json val users: List<GuiUserData>
)

data class DmStatisticsUserData(
  @Json val uid: String,
  @Json val rank: Int,

  @Json val score: Int,
  @Json val kills: Int,
  @Json val deaths: Int,

  @Json val chatModeratorLevel: Int = 0
)

data class InitDmStatisticsData(
  @Json val users: List<DmStatisticsUserData>
)

data class InventoryItemData(
  @Json val id: String,
  @Json val count: Int,
  @Json val slotId: Int,

  @Json val itemEffectTime: Int,
  @Json val itemRestSec: Int
)

data class InitInventoryData(
  @Json val items: List<InventoryItemData>
)

data class MineResourcesData(
  @Json val activateSound: Int = 389057,
  @Json val deactivateSound: Int = 965887,
  @Json val explosionSound: Int = 175648,
  @Json val idleExplosionTexture: Int = 545261,
  @Json val mainExplosionTexture: Int = 965737,
  @Json val blueMineTexture: Int = 925137,
  @Json val redMineTexture: Int = 342637,
  @Json val enemyMineTexture: Int = 975465,
  @Json val friendlyMineTexture: Int = 523632,
  @Json val explosionMarkTexture: Int = 962237,
  @Json val model3ds: Int = 895671
)

data class InitMineModelSettings(
  @Json val activationTimeMsec: Int = 1000,
  @Json val farVisibilityRadius: Int = 10,
  @Json val nearVisibilityRadius: Int = 7,
  @Json val impactForce: Int = 3,
  @Json val minDistanceFromBase: Int = 5,
  @Json val radius: Double = 0.5,
  @Json val minDamage: Int = 120,
  @Json val maxDamage: Int = 240,
  @Json val resources: MineResourcesData = MineResourcesData()
)

data class InitMineModelData(
  @Json val mines: List<Any> = listOf() // TODO(Assasans)
)

data class InitTankData(
  @Json val battleId: String,
  @Json val hull_id: String,
  @Json val turret_id: String,
  @Json val colormap_id: Int,
  @Json val partsObject: String,
  @Json val hullResource: Int,
  @Json val turretResource: Int,
  @Json val sfxData: String,
  @Json val position: String = "0.0@0.0@0.0@0.0",
  @Json val incration: Int = 2,
  @Json val tank_id: String,
  @Json val nickname: String,
  @Json val team_type: BattleTeam,
  @Json val state: String = "active",
  @Json val maxSpeed: Double,
  @Json val maxTurnSpeed: Double,
  @Json val acceleration: Double,
  @Json val reverseAcceleration: Double,
  @Json val sideAcceleration: Double,
  @Json val turnAcceleration: Double,
  @Json val reverseTurnAcceleration: Double,
  @Json val mass: Int,
  @Json val power: Double,
  @Json val dampingCoeff: Int,
  @Json val turret_turn_speed: Double,
  @Json val health: Int = 10000,
  @Json val rank: Int = 4,
  @Json val kickback: Double,
  @Json val turretTurnAcceleration: Double,
  @Json val impact_force: Double,
  @Json val state_null: Boolean = true
)

data class UpdatePlayerStatisticsData(
  @Json val id: String,
  @Json val rank: Int,
  @Json val team_type: BattleTeam,

  @Json val score: Int,
  @Json val kills: Int,
  @Json val deaths: Int
)

data class InitEffectsData(
  @Json val effects: List<Any> = listOf() // TODO(Assasans)
)

data class SpawnTankData(
  @Json val tank_id: String,
  @Json val health: Int,
  @Json val speed: Double,
  @Json val turn_speed: Double,
  @Json val turret_rotation_speed: Double,
  @Json val turretTurnAcceleration: Double,
  @Json val acceleration: Double,
  @Json val reverseAcceleration: Double,
  @Json val sideAcceleration: Double,
  @Json val turnAcceleration: Double,
  @Json val reverseTurnAcceleration: Double,
  @Json val incration_id: Int,
  @Json val team_type: BattleTeam,
  @Json val x: Double,
  @Json val y: Double,
  @Json val z: Double,
  @Json val rot: Double
)

data class TankSoundsData(
  @Json val engineIdleSound: Int = 386284,
  @Json val engineStartMovingSound: Int = 226985,
  @Json val engineMovingSound: Int = 75329,
  @Json val turretSound: Int = 242699
)

data class ChangeTankSpecificationData(
  @Json var speed: Double,
  @Json var turnSpeed: Double,
  @Json var turretRotationSpeed: Double,
  @Json var turretTurnAcceleration: Double,
  @Json var acceleration: Double,
  @Json var reverseAcceleration: Double,
  @Json var sideAcceleration: Double,
  @Json var turnAcceleration: Double,
  @Json var reverseTurnAcceleration: Double,
  @Json var dampingCoeff: Int,
  @Json var immediate: Boolean
) {
  companion object {
    fun fromPhysics(hull: HullPhysics, weapon: WeaponPhysics): ChangeTankSpecificationData {
      return ChangeTankSpecificationData(
        speed = hull.speed,
        turnSpeed = hull.turnSpeed,
        turretRotationSpeed = weapon.turretRotationSpeed,
        turretTurnAcceleration = weapon.turretTurnAcceleration,
        acceleration = hull.acceleration,
        reverseAcceleration = hull.reverseAcceleration,
        sideAcceleration = hull.sideAcceleration,
        turnAcceleration = hull.turnAcceleration,
        reverseTurnAcceleration = hull.reverseTurnAcceleration,
        dampingCoeff = hull.damping,
        immediate = true
      )
    }
  }
}
