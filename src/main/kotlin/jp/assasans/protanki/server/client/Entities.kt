package jp.assasans.protanki.server.client

import com.squareup.moshi.Json

data class GuiUserData(
  @Json val nickname: String,
  @Json val rank: Int,
  @Json val teamType: String
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
  @Json
  val sfxData: String = "{\"chargingPart1\":114424,\"chargingPart2\":468379,\"chargingPart3\":932241,\"hitMarkTexture\":670581,\"powTexture\":963502,\"ringsTexture\":966691,\"shotSound\":900596,\"smokeImage\":882103,\"sphereTexture\":212409,\"trailImage\":550305,\"lighting\":[{\"name\":\"charge\",\"light\":[{\"attenuationBegin\":200,\"attenuationEnd\":200,\"color\":5883129,\"intensity\":0.7,\"time\":0},{\"attenuationBegin\":200,\"attenuationEnd\":800,\"color\":5883129,\"intensity\":0.3,\"time\":600}]},{\"name\":\"shot\",\"light\":[{\"attenuationBegin\":100,\"attenuationEnd\":600,\"color\":5883129,\"intensity\":0.7,\"time\":0},{\"attenuationBegin\":1,\"attenuationEnd\":2,\"color\":5883129,\"intensity\":0,\"time\":300}]},{\"name\":\"hit\",\"light\":[{\"attenuationBegin\":200,\"attenuationEnd\":600,\"color\":5883129,\"intensity\":0.7,\"time\":0},{\"attenuationBegin\":1,\"attenuationEnd\":2,\"color\":5883129,\"intensity\":0,\"time\":300}]},{\"name\":\"rail\",\"light\":[{\"attenuationBegin\":100,\"attenuationEnd\":500,\"color\":5883129,\"intensity\":0.5,\"time\":0},{\"attenuationBegin\":1,\"attenuationEnd\":2,\"color\":5883129,\"intensity\":0,\"time\":1800}]}],\"bcsh\":[{\"brightness\":0,\"contrast\":0,\"saturation\":0,\"hue\":0,\"key\":\"trail\"},{\"brightness\":0,\"contrast\":0,\"saturation\":0,\"hue\":0,\"key\":\"charge\"}]}",
  @Json val position: String = "0.0@0.0@0.0@0.0",
  @Json val incration: Int = 2,
  @Json val tank_id: String,
  @Json val nickname: String,
  @Json val team_type: String,
  @Json val state: String = "active",
  @Json val maxSpeed: Int = 8,
  @Json val maxTurnSpeed: Double = 1.3229597,
  @Json val acceleration: Double = 9.09,
  @Json val reverseAcceleration: Double = 11.74,
  @Json val sideAcceleration: Double = 7.74,
  @Json val turnAcceleration: Double = 2.2462387,
  @Json val reverseTurnAcceleration: Double = 3.6576867,
  @Json val mass: Int = 1761,
  @Json val power: Double = 9.09,
  @Json val dampingCoeff: Int = 1500,
  @Json val turret_turn_speed: Double = 0.9815731713216109,
  @Json val health: Int = 10000,
  @Json val rank: Int = 4,
  @Json val kickback: Double = 2.138,
  @Json val turretTurnAcceleration: Double = 1.214225560612455,
  @Json val impact_force: Double = 3.6958,
  @Json val state_null: Boolean = true
)

data class UpdatePlayerStatisticsData(
  @Json val id: String,
  @Json val rank: Int,
  @Json val team_type: String,

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
  @Json val speed: Int = 8,
  @Json val turn_speed: Double = 1.3229597,
  @Json val turret_rotation_speed: Double = 0.9815731713216109,
  @Json val turretTurnAcceleration: Double = 1.214225560612455,
  @Json val acceleration: Double = 9.09,
  @Json val reverseAcceleration: Double = 11.74,
  @Json val sideAcceleration: Double = 7.74,
  @Json val turnAcceleration: Double = 2.2462387,
  @Json val reverseTurnAcceleration: Double = 3.6576867,
  @Json val incration_id: Int,
  @Json val team_type: String,
  @Json val x: Double,
  @Json val y: Double,
  @Json val z: Double,
  @Json val rot: Double
)
