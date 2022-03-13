package jp.assasans.protanki.server.battles

import java.util.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.client.railgun.FireTarget
import jp.assasans.protanki.server.client.railgun.ShotTarget
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.math.Quaternion
import jp.assasans.protanki.server.math.Vector3

interface ITickHandler {
  suspend fun tick() {}
}

enum class TankState {
  Dead,
  Respawn,
  SemiActive,
  Active
}

enum class ItemModification(val key: String) {
  M0("m0"),
  M1("m1"),
  M2("m2"),
  M3("m3");

  companion object {
    private val map = values().associateBy(ItemModification::key)

    fun get(key: String) = map[key]
  }
}

interface IItem {
  val player: BattlePlayer

  val name: String
  val resource: Int
}

interface IItemUpgradeable : IItem {
  var modification: ItemModification

  val fullName
    get() = "${name}_${modification.key}"
}

abstract class TankHull(
  override val player: BattlePlayer,
  override val name: String,
  override val resource: Int,
  override var modification: ItemModification
) : IItemUpgradeable {
}

class HunterHull(
  player: BattlePlayer,
  resource: Int,
  modification: ItemModification
) : TankHull(player, name = "hunter", resource, modification)

abstract class TankWeapon(
  override val player: BattlePlayer,
  override val name: String,
  override val resource: Int,
  override var modification: ItemModification
) : IItemUpgradeable {
}

class TankColoring(
  override val player: BattlePlayer,
  override val name: String,
  override val resource: Int
) : IItem {}

class RailgunWeapon(
  player: BattlePlayer,
  resource: Int,
  modification: ItemModification
) : TankWeapon(player, name = "railgun", resource, modification) {
  suspend fun fireStart() {
    val tank = player.tank ?: throw Exception("No Tank")

    Command(CommandName.StartFire, listOf(tank.id)).sendTo(tank.player.battle)
  }

  suspend fun fireTarget(target: FireTarget) {
    val tank = player.tank ?: throw Exception("No Tank")

    Command(
      CommandName.ShotTarget,
      listOf(
        tank.id,
        ShotTarget(target).toJson()
      )
    ).sendTo(tank.player.battle)
  }
}

class BattleTank(
  val id: String,
  val player: BattlePlayer,
  val incarnation: Int = 1,
  var state: TankState,
  var position: Vector3,
  var orientation: Quaternion,
  val hull: TankHull,
  val weapon: TankWeapon,
  val coloring: TankColoring
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
    // Command(CommandName.ActivateTank, listOf(id)).send(socket)
  }
}

enum class BattleTeam(val id: Int, val key: String) {
  Red(0, "RED"),
  Blue(1, "BLUE"),

  None(2, "NONE");

  companion object {
    private val map = values().associateBy(BattleTeam::key)

    fun get(key: String) = map[key]
  }
}

class BattlePlayer(
  val socket: UserSocket,
  val battle: Battle,
  var team: BattleTeam,
  var tank: BattleTank? = null,
  val isSpectator: Boolean = false,
  var score: Int = 0,
  var kills: Int = 0,
  var deaths: Int = 0
) : ITickHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  var incarnation: Int = 0

  val user: User
    get() = socket.user ?: throw Exception("Missing User")

  suspend fun init() {
    Command(
      CommandName.InitBonusesData,
      listOf(
        InitBonusesDataData(
          bonuses = listOf(
            BonusData(
              lighting = BonusLightingData(color = 6250335),
              id = "nitro",
              resourceId = 170010
            ),
            BonusData(
              lighting = BonusLightingData(color = 7185722),
              id = "armor",
              resourceId = 170006
            ),
            BonusData(
              lighting = BonusLightingData(color = 14605789),
              id = "health",
              resourceId = 170009
            ),
            BonusData(
              lighting = BonusLightingData(color = 8756459),
              id = "crystall",
              resourceId = 170007
            ),
            BonusData(
              lighting = BonusLightingData(color = 15044128),
              id = "gold",
              resourceId = 170008
            )
          )
        ).toJson()
      )
    ).send(socket)

    Command(
      CommandName.InitBattleModel,
      listOf(
        InitBattleModelData(
          battleId = battle.id,
          map_id = battle.map.name,
          mapId = battle.map.id,
          spectator = isSpectator,
          skybox = "{\"top\":45572,\"front\":57735,\"back\":268412,\"bottom\":31494,\"left\":927961,\"right\":987391}",
          map_graphic_data = "{\"mapId\":\"map_sandbox\",\"mapTheme\":\"SUMMER\",\"angleX\":-0.8500000238418579,\"angleZ\":2.5,\"lightColor\":13090219,\"shadowColor\":5530735,\"fogAlpha\":0.25,\"fogColor\":10543615,\"farLimit\":10000,\"nearLimit\":5000,\"gravity\":1000,\"skyboxRevolutionSpeed\":0,\"ssaoColor\":2045258,\"dustAlpha\":0.75,\"dustDensity\":0.15000000596046448,\"dustFarDistance\":7000,\"dustNearDistance\":5000,\"dustParticle\":\"summer\",\"dustSize\":200}"
        ).toJson()
      )
    ).send(socket)

    Command(
      CommandName.InitBonuses,
      listOf(
        listOf<InitBonusesData>().toJson()
      )
    ).send(socket)
  }

  suspend fun initLocal() {
    if(!isSpectator) {
      Command(CommandName.InitSuicideModel, listOf(10000.toString())).send(socket)
      Command(CommandName.InitStatisticsModel, listOf(battle.title)).send(socket)
    }

    Command(
      CommandName.InitGuiModel,
      listOf(
        InitGuiModelData(
          name = battle.title,
          fund = battle.fund,
          scoreLimit = 300,
          timeLimit = 600,
          currTime = 212,
          team = team != BattleTeam.None,
          users = battle.players.users().map { player ->
            GuiUserData(
              nickname = player.user.username,
              rank = player.user.rank.value,
              teamType = player.team.key
            )
          }
        ).toJson()
      )
    ).send(socket)
  }

  var stage2Initialized = false

  suspend fun initStage2() {
    if(isSpectator) {
      Command(
        CommandName.UpdateSpectatorsList,
        listOf(
          UpdateSpectatorsListData(
            spects = listOf(user.username)
          ).toJson()
        )
      )
    }

    Command(
      CommandName.InitDmStatistics,
      listOf(
        InitDmStatisticsData(
          users = battle.players.users().map { player ->
            DmStatisticsUserData(
              uid = player.user.username,
              rank = player.user.rank.value,
              score = player.score,
              kills = player.kills,
              deaths = player.deaths
            )
          }
        ).toJson()
      )
    ).send(socket)

    battle.players.forEach { player ->
      if(player == this) return@forEach

      Command(CommandName.BattlePlayerJoinDm, listOf(
        BattlePlayerJoinDmData(
          id = user.username,
          players = battle.players.users().map { player ->
            DmStatisticsUserData(
              uid = user.username,
              rank = user.rank.value,
              score = score,
              kills = kills,
              deaths = deaths
            )
          }
        ).toJson()
      )).send(player)
    }

    // TODO(Assasans)
    if(isSpectator) {
      Command(
        CommandName.InitInventory,
        listOf(
          InitInventoryData(items = listOf()).toJson()
        )
      ).send(socket)
    } else {
      Command(
        CommandName.InitInventory,
        listOf(
          InitInventoryData(
            items = listOf(
              InventoryItemData(
                id = "health",
                count = 1000,
                slotId = 1,
                itemEffectTime = 20,
                itemRestSec = 20
              ),
              InventoryItemData(
                id = "armor",
                count = 1000,
                slotId = 2,
                itemEffectTime = 55,
                itemRestSec = 20
              ),
              InventoryItemData(
                id = "double_damage",
                count = 1000,
                slotId = 3,
                itemEffectTime = 55,
                itemRestSec = 20
              ),
              InventoryItemData(
                id = "n2o",
                count = 1000,
                slotId = 4,
                itemEffectTime = 55,
                itemRestSec = 20
              ),
              InventoryItemData(
                id = "mine",
                count = 1000,
                slotId = 5,
                itemEffectTime = 20,
                itemRestSec = 20
              )
            )
          ).toJson()
        )
      ).send(socket)
    }

    Command(
      CommandName.InitMineModel,
      listOf(
        InitMineModelSettings().toJson(),
        InitMineModelData().toJson()
      )
    ).send(socket)

    initTanks()

    if(!isSpectator) {
      // Command(
      //   CommandName.InitTank,
      //   listOf(
      //     InitTankData(
      //       battleId = battle.id,
      //       hull_id = "hunter_m0",
      //       turret_id = "railgun_m0",
      //       colormap_id = 966681,
      //       hullResource = 227169,
      //       turretResource = 906685,
      //       partsObject = "{\"engineIdleSound\":386284,\"engineStartMovingSound\":226985,\"engineMovingSound\":75329,\"turretSound\":242699}",
      //       tank_id = (tank ?: throw Exception("No Tank")).id,
      //       nickname = user.username,
      //       team_type = team.key
      //     ).toJson()
      //   )
      // ).send(socket)

      logger.info { "Load stage 2" }

      Command(
        CommandName.UpdatePlayerStatistics,
        listOf(
          UpdatePlayerStatisticsData(
            id = (tank ?: throw Exception("No Tank")).id,
            rank = user.rank.value,
            team_type = team.key,
            score = score,
            kills = kills,
            deaths = deaths
          ).toJson()
        )
      ).send(socket)
    }

    Command(
      CommandName.InitEffects,
      listOf(
        InitEffectsData().toJson()
      )
    ).send(socket)

    if(!isSpectator) {
      Command(
        CommandName.PrepareToSpawn,
        listOf(
          (tank ?: throw Exception("No Tank")).id,
          "0.0@0.0@1000.0@0.0"
        )
      ).send(socket)

      Command(
        CommandName.ChangeHealth,
        listOf(
          (tank ?: throw Exception("No Tank")).id,
          10000.toString()
        )
      ).send(socket)

      // Command(
      //   CommandName.SpawnTank,
      //   listOf(
      //     SpawnTankData(
      //       tank_id = (tank ?: throw Exception("No Tank")).id,
      //       health = 10000,
      //       incration_id = incarnation,
      //       team_type = team.key,
      //       x = 0.0,
      //       y = 0.0,
      //       z = 1000.0,
      //       rot = 0.0
      //     ).toJson()
      //   )
      // ).send(socket)
    }

    spawnTanks()

    if(isSpectator) {
      battle.players.users().forEach { player ->
        if(player == this) return@forEach

        val tank = player.tank
        if(tank != null) {
          Command(CommandName.ActivateTank, listOf(tank.id)).send(socket)
        }
      }
    }

    if(!isSpectator) {
      (tank ?: throw Exception("No Tank")).activate()
    }
  }

  suspend fun initTanks() {
    battle.players.forEach { player ->
      // Init other players to self
      if(player != this && !player.isSpectator) {
        val tank = player.tank ?: throw Exception("No Tank")

        Command(
          CommandName.InitTank,
          listOf(
            InitTankData(
              battleId = battle.id,
              hull_id = tank.hull.fullName,
              turret_id = tank.weapon.fullName,
              colormap_id = tank.coloring.resource,
              hullResource = tank.hull.resource,
              turretResource = tank.weapon.resource,
              partsObject = "{\"engineIdleSound\":386284,\"engineStartMovingSound\":226985,\"engineMovingSound\":75329,\"turretSound\":242699}",
              tank_id = tank.id,
              nickname = player.user.username,
              team_type = player.team.key
            ).toJson()
          )
        ).send(socket)
      }

      // Init self to others
      if(!isSpectator) {
        val tank = tank ?: throw Exception("No Tank")

        Command(
          CommandName.InitTank,
          listOf(
            InitTankData(
              battleId = battle.id,
              hull_id = tank.hull.fullName,
              turret_id = tank.weapon.fullName,
              colormap_id = tank.coloring.resource,
              hullResource = tank.hull.resource,
              turretResource = tank.weapon.resource,
              partsObject = "{\"engineIdleSound\":386284,\"engineStartMovingSound\":226985,\"engineMovingSound\":75329,\"turretSound\":242699}",
              tank_id = tank.id,
              nickname = user.username,
              team_type = team.key
            ).toJson()
          )
        ).send(player)
      }
    }
  }

  suspend fun spawnTanks() {
    battle.players.forEach { player ->
      // Spawn other players for self
      if(player != this && !player.isSpectator) {
        Command(
          CommandName.SpawnTank,
          listOf(
            SpawnTankData(
              tank_id = (player.tank ?: throw Exception("No Tank")).id,
              health = 10000,
              incration_id = player.incarnation,
              team_type = player.team.key,
              x = 0.0,
              y = 0.0,
              z = 1000.0,
              rot = 0.0
            ).toJson()
          )
        ).send(socket)
      }

      // Spawn self for other players
      if(!isSpectator) {
        Command(
          CommandName.SpawnTank,
          listOf(
            SpawnTankData(
              tank_id = (tank ?: throw Exception("No Tank")).id,
              health = 10000,
              incration_id = incarnation,
              team_type = team.key,
              x = 0.0,
              y = 0.0,
              z = 1000.0,
              rot = 0.0
            ).toJson()
          )
        ).send(player)
      }
    }
  }

  suspend fun spawn(): BattleTank {
    incarnation++

    val tank = BattleTank(
      id = user.username,
      player = this,
      incarnation = incarnation,
      state = TankState.Respawn,
      position = Vector3(0.0, 0.0, 1000.0),
      orientation = Quaternion(),
      hull = HunterHull(player = this, resource = 227169, modification = ItemModification.M0),
      weapon = RailgunWeapon(player = this, resource = 906685, modification = ItemModification.M0),
      coloring = TankColoring(player = this, name = "green", resource = 966681)
    )

    this.tank = tank
    return tank
  }
}

class BattleMap(
  val id: Int,
  val name: String,
  val preview: Int
)

enum class SendTarget {
  Players,
  Spectators
}

suspend fun Command.sendTo(
  battle: Battle,
  vararg targets: SendTarget = arrayOf(SendTarget.Players, SendTarget.Spectators)
) = battle.sendTo(this, *targets)

fun List<BattlePlayer>.users() = filter { player -> !player.isSpectator }
fun List<BattlePlayer>.spectators() = filter { player -> player.isSpectator }

class Battle(
  val id: String,
  val title: String,
  var map: BattleMap,
  var fund: Int = 1337228
) : ITickHandler {
  companion object {
    private var lastId: Int = 1

    fun generateId(): String {
      return "test-${lastId++}"
    }
  }

  private val logger = KotlinLogging.logger { }

  val players: MutableList<BattlePlayer> = mutableListOf()

  suspend fun selectFor(socket: UserSocket) {
    Command(
      CommandName.SelectBattle,
      listOf(
        id
      )
    ).send(socket)
  }

  suspend fun showInfoFor(socket: UserSocket) {
    Command(
      CommandName.ShowBattleInfo,
      listOf(
        ShowDmBattleInfoData(
          itemId = id,
          battleMode = "DM",
          scoreLimit = 300,
          timeLimitInSec = 600,
          timeLeftInSec = 212,
          preview = map.preview,
          maxPeopleCount = 8,
          name = title,
          minRank = 0,
          maxRank = 16,
          spectator = true,
          withoutBonuses = false,
          withoutCrystals = false,
          withoutSupplies = false,
          users = listOf(
            BattleUser(user = "Luminate", kills = 666, score = 1337)
          ),
          score = 123
        ).toJson()
      )
    ).send(socket)
  }

  suspend fun sendTo(
    command: Command,
    vararg targets: SendTarget = arrayOf(SendTarget.Players, SendTarget.Spectators)
  ) {
    if(targets.contains(SendTarget.Players)) {
      players
        .users()
        .forEach { player -> command.send(player) }
    }
    if(targets.contains(SendTarget.Spectators)) {
      players
        .spectators()
        .forEach { player -> command.send(player) }
    }
  }

  override suspend fun tick() {
    players.forEach { player ->
      logger.trace { "Running tick handler for player ${player.user.username}" }
      player.tick()
    }
  }
}

interface IBattleProcessor : ITickHandler {
  val battles: MutableList<Battle>

  fun getBattle(id: String): Battle?
}

class BattleProcessor : IBattleProcessor {
  private val logger = KotlinLogging.logger { }

  override val battles: MutableList<Battle> = mutableListOf()

  override fun getBattle(id: String): Battle? = battles.singleOrNull { battle -> battle.id == id }

  override suspend fun tick() {
    battles.forEach { battle ->
      logger.trace { "Running tick handler for battle ${battle.id}" }
      battle.tick()
    }
  }
}
