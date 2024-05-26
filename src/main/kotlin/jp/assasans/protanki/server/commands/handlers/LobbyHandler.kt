package jp.assasans.protanki.server.commands.handlers

import kotlin.coroutines.coroutineContext
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.sync.withPermit
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import jp.assasans.protanki.server.*
import jp.assasans.protanki.server.battles.*
import jp.assasans.protanki.server.battles.map.IMapRegistry
import jp.assasans.protanki.server.battles.map.get
import jp.assasans.protanki.server.battles.map.getProplib
import jp.assasans.protanki.server.battles.map.getSkybox
import jp.assasans.protanki.server.battles.mode.*
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.*
import jp.assasans.protanki.server.exceptions.NoSuchProplibException
import jp.assasans.protanki.server.extensions.launchDelayed
import jp.assasans.protanki.server.quests.JoinBattleMapQuest
import jp.assasans.protanki.server.quests.questOf

/*
Battle exit:
-> switch_battle_select
<- unload_battle
-> i_exit_from_battle
<- remove_player_from_battle [{"battleId":"8405f22972a7a3b1","id":"Assasans"}]
<- releaseSlot [8405f22972a7a3b1, Assasans]
<- init_messages
<- notify_user_leave_battle [Assasans]
* load lobby resources *
<- init_battle_create
<- init_battle_select
<- select [8405f22972a7a3b1]
<- show_battle_info [{{"itemId":"8405f22972a7a3b1", ...}]
*/

class LobbyHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val battleProcessor by inject<IBattleProcessor>()
  private val resourceManager by inject<IResourceManager>()
  private val resourceConverter by inject<IResourceConverter>()
  private val mapRegistry by inject<IMapRegistry>()
  private val server by inject<ISocketServer>()
  private val userRepository by inject<IUserRepository>()
  private val userSubscriptionManager by inject<IUserSubscriptionManager>()

  @CommandHandler(CommandName.SelectBattle)
  suspend fun selectBattle(socket: UserSocket, id: String) {
    if(id == "null") return // Client side error, consequences of which (empty window) cannot be fixed by the server

    val battle = battleProcessor.getBattle(id) ?: throw Exception("Battle $id not found")

    logger.debug { "Select battle $id -> ${battle.title}" }

    socket.selectedBattle = battle
    battle.selectFor(socket)
    battle.showInfoFor(socket)
  }

  @CommandHandler(CommandName.ShowDamageEnabled)
  suspend fun showDamageEnabled(socket: UserSocket, id: String) {
    // TODO(Assasans)
  }

  @CommandHandler(CommandName.Fight)
  @ArgsBehaviour(ArgsBehaviourType.Raw)
  suspend fun fight(socket: UserSocket, args: CommandArgs) {
    socket.battleJoinLock.withPermit {
      if(socket.screen == Screen.Battle) return@withPermit // Client-side bug

      val user = socket.user ?: throw Exception("No User")
      val battle = socket.selectedBattle ?: throw Exception("Battle is not selected")

      val team = if(args.size == 1) {
        val rawTeam = args.get(0)
        BattleTeam.get(rawTeam) ?: throw Exception("Unknown team: $rawTeam")
      } else BattleTeam.None

      val minRank = battle.properties[BattleProperty.MinRank]
      val maxRank = battle.properties[BattleProperty.MaxRank]
      if(user.rank.value !in minRank..maxRank) {
        logger.warn { "Player ${user.username} attempted to join battle with incorrect rank: (user=${user.rank.value}, min=$minRank, max=$maxRank)" }
        return
      }

      socket.screen = Screen.Battle

      val player = BattlePlayer(
        coroutineContext = coroutineContext,
        socket = socket,
        battle = battle,
        team = team
      )
      battle.players.add(player)

      if(!player.isSpectator) {
        when(battle.modeHandler) {
          is DeathmatchModeHandler -> Command(CommandName.ReserveSlotDm, battle.id, player.user.username)
          is TeamModeHandler       -> Command(CommandName.ReserveSlotTeam, battle.id, player.user.username, team.key)
          else                     -> throw IllegalStateException("Unknown battle mode: ${battle.modeHandler::class}")
        }.let { command ->
          server.players
            .filter { player -> player.screen == Screen.BattleSelect }
            .filter { player -> player.active }
            .forEach { player -> command.send(player) }
        }

        Command(
          CommandName.NotifyPlayerJoinBattle,
          NotifyPlayerJoinBattleData(
            userId = player.user.username,
            battleId = battle.id,
            mapName = battle.title,
            mode = battle.modeHandler.mode,
            privateBattle = false,
            proBattle = false,
            minRank = battle.properties[BattleProperty.MinRank],
            maxRank = battle.properties[BattleProperty.MaxRank]
          ).toJson()
        ).let { command ->
          server.players
            .filter { player -> player.screen == Screen.BattleSelect }
            .filter { player -> player.active }
            .forEach { player -> command.send(player) }
        }

        Command(
          when(battle.modeHandler) {
            is DeathmatchModeHandler -> CommandName.AddBattlePlayerDm
            is TeamModeHandler       -> CommandName.AddBattlePlayerTeam
            else                     -> throw IllegalStateException("Unknown battle mode: ${battle.modeHandler::class}")
          },
          AddBattlePlayerData(
            battleId = battle.id,
            kills = player.kills,
            score = player.score,
            suspicious = false,
            user = player.user.username,
            type = player.team
          ).toJson()
        ).let { command ->
          server.players
            .filter { player -> player.screen == Screen.BattleSelect && player.selectedBattle == battle }
            .filter { player -> player.active }
            .forEach { player -> command.send(player) }
        }
      }

      socket.initBattleLoad()

      Command(CommandName.InitShotsData, resourceManager.get("shots-data.json").readText()).send(socket)

      socket.loadDependency(
        ClientResources(
          battle.map.resources.proplibs
            .map { proplib ->
              try {
                mapRegistry.getProplib(proplib)
              } catch(exception: NoSuchElementException) {
                throw NoSuchProplibException(proplib, "${battle.map.name}@${battle.map.theme.name}", exception)
              }
            }
            .map(ServerProplib::toServerResource)
            .map(resourceConverter::toClientResource)
        ).toJson()
      ).await()
      socket.loadDependency(
        ClientResources(
          mapRegistry.getSkybox(battle.map.skybox)
            .map { (_, resource) -> resource.toServerResource(ResourceType.Image) }
            .map(resourceConverter::toClientResource)
        ).toJson()
      ).await()
      socket.loadDependency(
        ClientResources(
          listOf(battle.map.resources.map.toServerResource(ResourceType.Map).toClientResource(resourceConverter))
        ).toJson()
      ).await()

      player.init()
      player.createTank()

      player.user.questOf<JoinBattleMapQuest> { quest -> quest.map == battle.map.name }?.let { quest ->
        quest.current++
        socket.updateQuests()
        quest.updateProgress()
      }
    }
  }

  @CommandHandler(CommandName.JoinAsSpectator)
  suspend fun joinAsSpectator(socket: UserSocket) {
    socket.battleJoinLock.withPermit {
      if(socket.screen == Screen.Battle) return@withPermit // Client-side bug

      val battle = socket.selectedBattle ?: throw Exception("Battle is not selected")

      socket.screen = Screen.Battle

      val player = BattlePlayer(
        coroutineContext = coroutineContext,
        socket = socket,
        battle = battle,
        team = BattleTeam.None,
        isSpectator = true
      )
      battle.players.add(player)

      socket.initBattleLoad()

      socket.loadDependency(
        ClientResources(
          battle.map.resources.proplibs
            .map { proplib ->
              try {
                mapRegistry.getProplib(proplib)
              } catch(exception: NoSuchElementException) {
                throw NoSuchProplibException(proplib, "${battle.map.name}@${battle.map.theme.name}", exception)
              }
            }
            .map(ServerProplib::toServerResource)
            .map(resourceConverter::toClientResource)
        ).toJson()
      ).await()
      socket.loadDependency(
        ClientResources(
          mapRegistry.getSkybox(battle.map.skybox)
            .map { (_, resource) -> resource.toServerResource(ResourceType.Image) }
            .map(resourceConverter::toClientResource)
        ).toJson()
      ).await()
      socket.loadDependency(
        ClientResources(
          listOf(battle.map.resources.map.toServerResource(ResourceType.Map).toClientResource(resourceConverter))
        ).toJson()
      ).await()

      player.init()
    }
  }

  @CommandHandler(CommandName.InitSpectatorUser)
  suspend fun initSpectatorUser(socket: UserSocket) {
    socket.battleJoinLock.withPermit {
      val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")

      if(player.initSpectatorUserCalled) return // Client-side bug
      player.initSpectatorUserCalled = true

      Command(
        CommandName.InitShotsData,
        resourceManager.get("shots-data.json").readText()
      ).send(socket) // TODO(Assasans): initBattleLoad?

      player.initLocal()
    }
  }

  @CommandHandler(CommandName.SwitchBattleSelect)
  suspend fun switchBattleSelect(socket: UserSocket) {
    logger.debug { "Switch to battle select" }

    val battle = socket.battle

    if(battle != null && socket.screen == Screen.BattleSelect) {
      // Return to battle

      socket.screen = Screen.Battle
      Command(CommandName.StartLayoutSwitch, "BATTLE").send(socket)
      Command(CommandName.UnloadBattleSelect).send(socket)
      Command(CommandName.EndLayoutSwitch, "BATTLE", "BATTLE").send(socket)
    } else {
      Command(CommandName.StartLayoutSwitch, "BATTLE_SELECT").send(socket)

      if(socket.screen == Screen.Garage) {
        Command(CommandName.UnloadGarage).send(socket)
      }

      socket.screen = Screen.BattleSelect
      socket.loadLobbyResources()

      Command(
        CommandName.EndLayoutSwitch,
        if(battle != null) "BATTLE" else "BATTLE_SELECT",
        "BATTLE_SELECT"
      ).send(socket)

      socket.initBattleList()

      val selectedBattle = socket.selectedBattle
      if(selectedBattle != null) {
        logger.debug { "Select battle ${selectedBattle.id} -> ${selectedBattle.title}" }

        selectedBattle.selectFor(socket)
        selectedBattle.showInfoFor(socket)
      }
    }
  }

  @CommandHandler(CommandName.SwitchGarage)
  suspend fun switchGarage(socket: UserSocket) {
    logger.debug { "Switch to garage" }

    val player = socket.battlePlayer
    if(player != null && socket.screen == Screen.Garage) {
      // Return to battle

      socket.screen = Screen.Battle
      Command(CommandName.StartLayoutSwitch, "BATTLE").send(socket)
      Command(CommandName.UnloadGarage).send(socket)
      Command(CommandName.EndLayoutSwitch, "BATTLE", "BATTLE").send(socket)

      val tank = player.tank
      if(tank != null && (tank.state == TankState.Active || tank.state == TankState.SemiActive)) {
        if(player.equipmentChanged && !tank.selfDestructing) {
          val delay = 3.seconds

          Command(CommandName.EquipmentChangedCountdown, delay.inWholeMilliseconds.toString()).send(socket)

          player.coroutineScope.launchDelayed(delay) {
            tank.selfDestruct(silent = true)
          }
        }
      } else {
        player.changeEquipment()
      }
    } else {
      Command(CommandName.StartLayoutSwitch, "GARAGE").send(socket)

      if(socket.screen == Screen.BattleSelect) {
        Command(CommandName.UnloadBattleSelect).send(socket)
      }

      socket.screen = Screen.Garage
      socket.loadGarageResources()
      socket.initGarage()

      Command(
        CommandName.EndLayoutSwitch,
        if(player != null) "BATTLE" else "GARAGE",
        "GARAGE"
      ).send(socket)
    }
  }

  @CommandHandler(CommandName.CreateBattle)
  suspend fun createBattle(socket: UserSocket, data: BattleCreateData) {
    val handler = when(data.battleMode) {
      BattleMode.Deathmatch     -> DeathmatchModeHandler.builder()
      BattleMode.TeamDeathmatch -> TeamDeathmatchModeHandler.builder()
      BattleMode.CaptureTheFlag -> CaptureTheFlagModeHandler.builder()
      BattleMode.ControlPoints  -> ControlPointsModeHandler.builder()
    }

    // TODO(Assasans): Advanced map configuration
    val battle = Battle(
      coroutineContext,
      id = Battle.generateId(),
      title = data.name,
      map = mapRegistry.get(data.mapId, ServerMapTheme.getByClient(data.theme) ?: throw Exception("Unknown theme: ${data.theme}")),
      modeHandlerBuilder = handler
    )
    if(data.parkourMode) {
      battle.properties[BattleProperty.ParkourMode] = true
      battle.properties[BattleProperty.DamageEnabled] = false
      battle.properties[BattleProperty.InstantSelfDestruct] = true
      battle.properties[BattleProperty.SuppliesCooldownEnabled] = false
    }
    if(data.proBattle) { // PRO-battle options have undefined value if proBattle is false
      battle.properties[BattleProperty.RearmingEnabled] = data.rearmingEnabled
    }
    battle.properties[BattleProperty.MinRank] = data.minRank
    battle.properties[BattleProperty.MaxRank] = data.maxRank
    battle.properties[BattleProperty.TimeLimit] = data.timeLimitInSec

    battleProcessor.battles.add(battle)

    Command(CommandName.AddBattle, battle.toBattleData().toJson()).let { command ->
      server.players
        .filter { player -> player.screen == Screen.BattleSelect }
        .filter { player -> player.active }
        .forEach { player -> command.send(player) }
    }


    socket.selectedBattle = battle
    battle.selectFor(socket)
    battle.showInfoFor(socket)
  }

  @CommandHandler(CommandName.CheckBattleName)
  suspend fun checkBattleName(socket: UserSocket, name: String) {
    // Pass-through
    Command(CommandName.SetCreateBattleName, name).send(socket)
  }

  /**
   * Notifications order:
   * - NotifyUserOnline
   * - NotifyUserRank
   * - NotifyPlayerJoinBattle / NotifyPlayerLeaveBattle
   * - NotifyUserPremium
   */
  @CommandHandler(CommandName.SubscribeUserUpdate)
  suspend fun subscribeUserUpdate(socket: UserSocket, username: String) {
    val target = server.players.singleOrNull { player -> player.user?.username == username }
    var targetUser = target?.user
    if(targetUser == null) {
      targetUser = userRepository.getUser(username) ?: throw Exception("User $username not found")
      logger.debug { "Fetched user $username from database" }
    }

    val subscription = userSubscriptionManager.getOrAdd(targetUser)

    // TODO(Assasans): Use StateFlow
    Command(
      CommandName.NotifyUserOnline,
      NotifyUserOnlineData(username = targetUser.username, online = target != null).toJson()
    ).send(socket)

    // TODO(Assasans): Save Job
    socket.coroutineScope.launch {
      subscription.rank.collect { rank ->
        Command(
          CommandName.NotifyUserRank,
          NotifyUserRankData(username = targetUser.username, rank = rank.value).toJson()
        ).send(socket)
      }
    }

    // TODO(Assasans): Maybe use StateFlow
    target?.battlePlayer?.let { player ->
      val battle = player.battle

      Command(
        CommandName.NotifyPlayerJoinBattle,
        NotifyPlayerJoinBattleData(
          userId = player.user.username,
          battleId = battle.id,
          mapName = battle.title,
          mode = battle.modeHandler.mode,
          privateBattle = false,
          proBattle = false,
          minRank = battle.properties[BattleProperty.MinRank],
          maxRank = battle.properties[BattleProperty.MaxRank]
        ).toJson()
      ).send(socket)
    }

    // TODO(Assasans): Use StateFlow
    Command(
      CommandName.NotifyUserPremium,
      NotifyUserPremiumData(username = targetUser.username, premiumTimeLeftInSeconds = -1).toJson()
    ).send(socket)
  }

  @CommandHandler(CommandName.UnsubscribeUserUpdate)
  suspend fun unsubscribeUserUpdate(socket: UserSocket, username: String) {
    // TODO(Assasans): Cancel saved jobs
  }
}
