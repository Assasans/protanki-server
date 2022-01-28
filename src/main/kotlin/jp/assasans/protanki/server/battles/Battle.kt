package jp.assasans.protanki.server.battles

import com.squareup.moshi.Moshi
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.client.*
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

class BattleTank(
  val id: String,
  val player: BattlePlayer,
  val incarnation: Int = 1,
  var state: TankState,
  var position: Vector3,
  var orientation: Quaternion
) : ITickHandler {
  private val logger = KotlinLogging.logger { }

  val socket: UserSocket
    get() = player.socket

  val battle: Battle
    get() = player.battle

  suspend fun activate() {
    state = TankState.Active
    Command(CommandName.ActivateTank, listOf(id)).send(socket)
  }
}

class BattlePlayer(
  val socket: UserSocket,
  val battle: Battle,
  var tank: BattleTank? = null,
  val isSpectator: Boolean = false
) : ITickHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val json by inject<Moshi>()

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
          battleId = "493202bf695cc88a",
          map_id = "map_sandbox",
          mapId = 663288,
          spectator = isSpectator,
          skybox = "{\"top\":45572,\"front\":57735,\"back\":268412,\"bottom\":31494,\"left\":927961,\"right\":987391}",
          map_graphic_data = "{\"mapId\":\"map_sandbox\",\"mapTheme\":\"SUMMER\",\"angleX\":-0.8500000238418579,\"angleZ\":2.5,\"lightColor\":13090219,\"shadowColor\":5530735,\"fogAlpha\":0.25,\"fogColor\":10543615,\"farLimit\":10000,\"nearLimit\":5000,\"gravity\":500,\"skyboxRevolutionSpeed\":0,\"ssaoColor\":2045258,\"dustAlpha\":0.75,\"dustDensity\":0.15000000596046448,\"dustFarDistance\":7000,\"dustNearDistance\":5000,\"dustParticle\":\"summer\",\"dustSize\":200}"
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
      Command(CommandName.InitSuicideModel, listOf("10000")).send(socket)
      Command(CommandName.InitStatisticsModel, listOf("For newbies")).send(socket)
    }

    Command(
      CommandName.InitGuiModel,
      listOf(
        InitGuiModelData(
          name = "ProTanki Server",
          fund = 1337228,
          scoreLimit = 300,
          timeLimit = 600,
          currTime = 212,
          team = false,
          users = listOf(
            GuiUserData(nickname = "roflanebalo", rank = 4, teamType = "NONE"),
            GuiUserData(nickname = "Luminate", rank = 16, teamType = "NONE")
          )
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
          users = listOf(
            DmStatisticsUserData(
              uid = "roflanebalo",
              rank = 4,
              score = 666,
              kills = 1000,
              deaths = 7
            ),
            DmStatisticsUserData(
              uid = "Luminate",
              rank = 16,
              score = 456,
              kills = 777,
              deaths = 333
            )
          )
        ).toJson()
      )
    ).send(socket)

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

    if(!isSpectator) {
      Command(
        CommandName.InitTank,
        listOf(
          InitTankData(
            battleId = "493202bf695cc88a",
            hull_id = "hunter_m0",
            turret_id = "railgun_m0",
            colormap_id = 966681,
            hullResource = 227169,
            turretResource = 906685,
            partsObject = "{\"engineIdleSound\":386284,\"engineStartMovingSound\":226985,\"engineMovingSound\":75329,\"turretSound\":242699}",
            tank_id = "roflanebalo",
            nickname = "roflanebalo",
            team_type = "NONE"
          ).toJson()
        )
      ).send(socket)

      logger.info { "Load stage 2" }

      Command(
        CommandName.UpdatePlayerStatistics,
        listOf(
          UpdatePlayerStatisticsData(
            id = "roflanebalo",
            rank = 4,
            team_type = "NONE",
            score = 666,
            kills = 1000,
            deaths = 777
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
          "roflanebalo",
          "0.0@0.0@1000.0@0.0"
        )
      ).send(socket)

      Command(
        CommandName.ChangeHealth,
        listOf(
          "roflanebalo",
          "10000"
        )
      ).send(socket)

      Command(
        CommandName.SpawnTank,
        listOf(
          SpawnTankData(
            tank_id = "roflanebalo",
            health = 10000,
            incration_id = 2,
            team_type = "NONE",
            x = 0.0,
            y = 0.0,
            z = 1000.0,
            rot = 0.0
          ).toJson()
        )
      ).send(socket)

      tank!!.activate()
    }
  }

  suspend fun spawn(): BattleTank {
    incarnation++

    val tank = BattleTank(
      id = socket.user!!.username,
      player = this,
      incarnation = incarnation,
      state = TankState.Respawn,
      position = Vector3(0.0, 0.0, 1000.0),
      orientation = Quaternion()
    )

    this.tank = tank
    return tank
  }
}

class Battle(
  val id: String,
  val title: String
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
          itemId = "493202bf695cc88a",
          battleMode = "DM",
          scoreLimit = 300,
          timeLimitInSec = 600,
          timeLeftInSec = 212,
          preview = 388954,
          maxPeopleCount = 8,
          name = "ProTanki Server",
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

  override suspend fun tick() {
    players.forEach { player ->
      logger.trace { "Running tick handler for player ${player.socket.user!!.username}" }
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
