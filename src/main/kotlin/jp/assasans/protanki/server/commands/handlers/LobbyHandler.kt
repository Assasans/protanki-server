package jp.assasans.protanki.server.commands.handlers

import kotlin.coroutines.coroutineContext
import kotlin.io.path.readText
import kotlinx.coroutines.sync.withPermit
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.*
import jp.assasans.protanki.server.battles.*
import jp.assasans.protanki.server.battles.map.IMapRegistry
import jp.assasans.protanki.server.battles.map.get
import jp.assasans.protanki.server.battles.map.getProplib
import jp.assasans.protanki.server.battles.mode.CaptureTheFlagModeHandler
import jp.assasans.protanki.server.battles.mode.ControlPointsModeHandler
import jp.assasans.protanki.server.battles.mode.DeathmatchModeHandler
import jp.assasans.protanki.server.battles.mode.TeamDeathmatchModeHandler
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.*

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

  @CommandHandler(CommandName.SelectBattle)
  suspend fun selectBattle(socket: UserSocket, id: String) {
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

      val battle = socket.selectedBattle ?: throw Exception("Battle is not selected")

      val team = if(args.size == 1) {
        val rawTeam = args.get(0)
        BattleTeam.get(rawTeam) ?: throw Exception("Unknown team: $rawTeam")
      } else BattleTeam.None

      socket.screen = Screen.Battle

      val player = BattlePlayer(
        coroutineContext = coroutineContext,
        socket = socket,
        battle = battle,
        team = team
      )
      battle.players.add(player)

      socket.initBattleLoad()

      Command(CommandName.InitShotsData, listOf(resourceManager.get("shots-data.json").readText())).send(socket)

      socket.awaitDependency(
        socket.loadDependency(
          ClientResources(
            battle.map.resources.proplibs
              .map(mapRegistry::getProplib)
              .map(ServerProplib::toServerResource)
              .map(resourceConverter::toClientResource)
          ).toJson()
        )
      )
      socket.awaitDependency(socket.loadDependency(ClientResources(battle.map.resources.skybox.map(resourceConverter::toClientResource)).toJson()))
      socket.awaitDependency(
        socket.loadDependency(
          ClientResources(
            listOf(battle.map.resources.map.toServerResource().toClientResource(resourceConverter))
          ).toJson()
        )
      )

      player.init()
      player.createTank()
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

      socket.awaitDependency(
        socket.loadDependency(
          ClientResources(
            battle.map.resources.proplibs
              .map(mapRegistry::getProplib)
              .map(ServerProplib::toServerResource)
              .map(resourceConverter::toClientResource)
          ).toJson()
        )
      )
      socket.awaitDependency(socket.loadDependency(ClientResources(battle.map.resources.skybox.map(resourceConverter::toClientResource)).toJson()))
      socket.awaitDependency(
        socket.loadDependency(
          ClientResources(
            listOf(battle.map.resources.map.toServerResource().toClientResource(resourceConverter))
          ).toJson()
        )
      )

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
        listOf(resourceManager.get("shots-data.json").readText())
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

      Command(CommandName.StartLayoutSwitch, listOf("BATTLE")).send(socket)
      Command(CommandName.UnloadBattleSelect).send(socket)
      Command(CommandName.EndLayoutSwitch, listOf("BATTLE", "BATTLE")).send(socket)
    } else {
      Command(CommandName.StartLayoutSwitch, listOf("BATTLE_SELECT")).send(socket)

      if(socket.screen == Screen.Garage) {
        Command(CommandName.UnloadGarage).send(socket)
      }

      socket.screen = Screen.BattleSelect
      socket.loadLobbyResources()

      Command(
        CommandName.EndLayoutSwitch, listOf(
          if(battle != null) "BATTLE" else "BATTLE_SELECT",
          "BATTLE_SELECT"
        )
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

    val battle = socket.battle

    if(battle != null && socket.screen == Screen.Garage) {
      // Return to battle

      Command(CommandName.StartLayoutSwitch, listOf("BATTLE")).send(socket)
      Command(CommandName.UnloadGarage).send(socket)
      Command(CommandName.EndLayoutSwitch, listOf("BATTLE", "BATTLE")).send(socket)
    } else {
      Command(CommandName.StartLayoutSwitch, listOf("GARAGE")).send(socket)

      if(socket.screen == Screen.BattleSelect) {
        Command(CommandName.UnloadBattleSelect).send(socket)
      }

      socket.screen = Screen.Garage
      socket.loadGarageResources()
      socket.initGarage()

      Command(
        CommandName.EndLayoutSwitch, listOf(
          if(battle != null) "BATTLE" else "GARAGE",
          "GARAGE"
        )
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

    battleProcessor.battles.add(battle)

    Command(CommandName.AddBattle, listOf(battle.toBattleData().toJson())).let { command ->
      server.players
        .filter { player -> player.screen == Screen.BattleSelect }
        .forEach { player -> command.send(player) }
    }


    socket.selectedBattle = battle
    battle.selectFor(socket)
    battle.showInfoFor(socket)
  }

  @CommandHandler(CommandName.CheckBattleName)
  suspend fun checkBattleName(socket: UserSocket, name: String) {
    // Pass-through
    Command(CommandName.SetCreateBattleName, listOf(name)).send(socket)
  }
}
