package jp.assasans.protanki.server.client

import com.squareup.moshi.Json
import jp.assasans.protanki.server.battles.BattleMode
import jp.assasans.protanki.server.battles.BattlePlayer
import jp.assasans.protanki.server.battles.BattleTeam
import jp.assasans.protanki.server.garage.HullPhysics
import jp.assasans.protanki.server.garage.WeaponPhysics
import jp.assasans.protanki.server.serialization.SerializeNull

data class GuiUserData(
  @Json val nickname: String,
  @Json val rank: Int,
  @Json val teamType: BattleTeam
)

enum class EquipmentConstraintsMode(val key: String) {
  None("NONE"),
  HornetRailgun("HORNET_RAILGUN"),
  WaspRailgun("WASP_RAILGUN"),
  HornetWaspRailgun("HORNET_WASP_RAILGUN");

  companion object {
    private val map = values().associateBy(EquipmentConstraintsMode::key)

    fun get(key: String) = map[key]
  }
}

data class InitGuiModelData(
  @Json val name: String,
  @Json val fund: Int,
  @Json val scoreLimit: Int,
  @Json val timeLimit: Int,
  @Json(name = "currTime") val timeLeft: Int,
  @Json val score_red: Int = 0,
  @Json val score_blue: Int = 0,
  @Json val team: Boolean, // Is team battle
  @Json val equipmentConstraintsMode: EquipmentConstraintsMode = EquipmentConstraintsMode.None,
  @Json val parkourMode: Boolean,
  @Json val battleType: BattleMode,
  @Json val users: List<GuiUserData>
)

fun List<BattlePlayer>.toStatisticsUsers(): List<StatisticsUserData> {
  return map { player ->
    StatisticsUserData(
      uid = player.user.username,
      rank = player.user.rank.value,
      score = player.score,
      kills = player.kills,
      deaths = player.deaths
    )
  }
}

data class StatisticsUserData(
  @Json val uid: String,
  @Json val rank: Int,

  @Json val score: Int,
  @Json val kills: Int,
  @Json val deaths: Int,

  @Json val chatModeratorLevel: ChatModeratorLevel = ChatModeratorLevel.None
)

data class InitDmStatisticsData(
  @Json val users: List<StatisticsUserData>
)

data class InitTeamStatisticsData(
  @Json val reds: List<StatisticsUserData>,
  @Json val blues: List<StatisticsUserData>,

  @Json val blueScore: Int,
  @Json val redScore: Int
)

data class CtfModelResources(
  @Json val blueFlagSprite: Int = 538453,
  @Json val bluePedestalModel: Int = 236578,
  @Json val redFlagSprite: Int = 44351,
  @Json val redPedestalModel: Int = 500060,
  @Json val flagDropSound: Int = 717912,
  @Json val flagReturnSound: Int = 694498,
  @Json val flagTakeSound: Int = 89214,
  @Json val winSound: Int = 525427
)

data class CtfModelLighting(
  @Json val redColor: Int = 16711680,
  @Json val redColorIntensity: Int = 1,
  @Json val blueColor: Int = 26367,
  @Json val blueColorIntensity: Int = 1,
  @Json val attenuationBegin: Int = 100,
  @Json val attenuationEnd: Int = 1000
)

data class InitCtfModelData(
  @Json val resources: String,
  @Json val lighting: String,
  @Json val basePosBlueFlag: Vector3Data,
  @Json val basePosRedFlag: Vector3Data,
  @Json val posBlueFlag: Vector3Data?,
  @Json val posRedFlag: Vector3Data?,
  @Json @SerializeNull val blueFlagCarrierId: String?,
  @Json @SerializeNull val redFlagCarrierId: String?
)

typealias InitFlagsData = InitCtfModelData // Same properties

data class FlagDroppedData(
  @Json val x: Double,
  @Json val y: Double,
  @Json val z: Double,
  @Json val flagTeam: BattleTeam
)

data class DomModelResources(
  @Json val bigLetters: Int = 150231,
  @Json val blueCircle: Int = 102373,
  @Json val bluePedestalTexture: Int = 915688,
  @Json val blueRay: Int = 560829,
  @Json val blueRayTip: Int = 546583,
  @Json val neutralCircle: Int = 982573,
  @Json val neutralPedestalTexture: Int = 298097,
  @Json val pedestal: Int = 992320,
  @Json val redCircle: Int = 474249,
  @Json val redPedestalTexture: Int = 199168,
  @Json val redRay: Int = 217165,
  @Json val redRayTip: Int = 370093,
  @Json val pointCapturedNegativeSound: Int = 240260,
  @Json val pointCapturedPositiveSound: Int = 567101,
  @Json val pointCaptureStartNegativeSound: Int = 832304,
  @Json val pointCaptureStartPositiveSound: Int = 345377,
  @Json val pointCaptureStopNegativeSound: Int = 730634,
  @Json val pointCaptureStopPositiveSound: Int = 930495,
  @Json val pointNeutralizedNegativeSound: Int = 650249,
  @Json val pointNeutralizedPositiveSound: Int = 752472,
  @Json val pointScoreDecreasingSound: Int = 679479,
  @Json val pointScoreIncreasingSound: Int = 752002
)

data class DomModelLighting(
  @Json val redPointColor: Int = 16711680,
  @Json val redPointIntensity: Int = 1,
  @Json val bluePointColor: Int = 26367,
  @Json val bluePointIntensity: Int = 1,
  @Json val neutralPointColor: Int = 16777215,
  @Json val neutralPointIntensity: Double = 0.7,
  @Json val attenuationBegin: Int = 100,
  @Json val attenuationEnd: Int = 1000
)

data class DomPoint(
  @Json val id: String,
  @Json val radius: Double,
  @Json val x: Double,
  @Json val y: Double,
  @Json val z: Double,
  @Json val score: Int,
  @Json val state: String,
  @Json val occupated_users: List<String>
)

data class InitDomModelData(
  @Json val resources: String,
  @Json val lighting: String,
  @Json val points: List<DomPoint>,
  @Json val mine_activation_radius: Int
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
  @Json val mines: List<AddMineData>
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
  @Json val state: String,
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
  @Json val health: Int,
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
  @Json val effects: List<TankEffectData>
)

data class TankEffectData(
  @Json val userID: String,
  @Json val itemIndex: Int,
  @Json val durationTime: Long,
  @Json val activeAfterDeath: Boolean = false,
  @Json val effectLevel: Int = 0
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

data class NotifyPlayerJoinBattleData(
  @Json val userId: String,
  @Json val battleId: String,
  @Json val mapName: String,
  @Json val mode: BattleMode,
  @Json val privateBattle: Boolean,
  @Json val proBattle: Boolean,
  @Json val minRank: Int,
  @Json val maxRank: Int
)

data class AddBattlePlayerData(
  @Json val battleId: String,
  @Json val kills: Int,
  @Json val score: Int,
  @Json val suspicious: Boolean,
  @Json val user: String,
  @Json val type: BattleTeam
)

data class AddMineData(
  @Json val mineId: String,
  @Json val userId: String,
  @Json val x: Double,
  @Json val y: Double,
  @Json val z: Double
)

data class BuyItemResponseData(
  @Json val itemId: String,
  @Json val count: Int,
  @Json val addable: Boolean = true
)

data class NotifyUserOnlineData(
  @Json(name = "userId") val username: String,
  @Json val online: Boolean
)

data class NotifyUserRankData(
  @Json(name = "userId") val username: String,
  @Json val rank: Int
)

data class NotifyUserPremiumData(
  @Json(name = "userId") val username: String,
  @Json val premiumTimeLeftInSeconds: Int
)

data class NotifyUserUsernameData(
  @Json(name = "userId") val username: String,
  @Json(name = "newUserId") val newUsername: String
)

data class FinishBattleData(
  @Json val time_to_restart: Long = 10000,
  @Json val users: List<FinishBattleUserData>
)

data class FinishBattleUserData(
  @Json(name = "id") val username: String,
  @Json val rank: Int,
  @Json(name = "team_type") val team: BattleTeam,

  @Json val score: Int,

  @Json val kills: Int,
  @Json val deaths: Int,

  @Json val prize: Int,
  @Json val bonus_prize: Int = 0
)
