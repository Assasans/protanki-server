package jp.assasans.protanki.server.commands.handlers

import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.bufferedReader
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.battles.BattlePlayer
import jp.assasans.protanki.server.battles.BattleTeam
import jp.assasans.protanki.server.battles.IBattleProcessor
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler

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

  @CommandHandler(CommandName.SelectBattle)
  suspend fun selectBattle(socket: UserSocket, id: String) {
    val battle = battleProcessor.getBattle(id) ?: throw Exception("Battle $id not found")

    logger.debug { "Select battle $id -> ${battle.title}" }

    socket.selectedBattle = battle
    battle.showInfoFor(socket)
  }

  @CommandHandler(CommandName.ShowDamageEnabled)
  suspend fun showDamageEnabled(socket: UserSocket, id: String) {
    // TODO(Assasans)
  }

  @CommandHandler(CommandName.Fight)
  suspend fun fight(socket: UserSocket) {
    // TODO(Assasans): Shit
    val resourcesMap1Reader = Paths.get("src/main/resources/resources/maps/sandbox-summer-1.json").absolute().bufferedReader()
    val resourcesMap1 = resourcesMap1Reader.use { it.readText() }

    // TODO(Assasans): Shit
    val resourcesMap2Reader = Paths.get("src/main/resources/resources/maps/sandbox-summer-2.json").absolute().bufferedReader()
    val resourcesMap2 = resourcesMap2Reader.use { it.readText() }

    // TODO(Assasans): Shit
    val resourcesMap3Reader = Paths.get("src/main/resources/resources/maps/sandbox-summer-3.json").absolute().bufferedReader()
    val resourcesMap3 = resourcesMap3Reader.use { it.readText() }

    // TODO(Assasans): Shit
    val shotsDataReader = Paths.get("src/main/resources/resources/shots-data.json").absolute().bufferedReader()
    val shotsData = shotsDataReader.use { it.readText() }

    val player = BattlePlayer(
      socket = socket,
      battle = battleProcessor.battles[0],
      team = BattleTeam.None
    )
    battleProcessor.battles[0].players.add(player)

    socket.initBattleLoad()

    Command(CommandName.InitShotsData, mutableListOf(shotsData)).send(socket)

    socket.awaitDependency(socket.loadDependency(resourcesMap1))
    socket.awaitDependency(socket.loadDependency(resourcesMap2))
    socket.awaitDependency(socket.loadDependency(resourcesMap3))

    player.init()
    player.spawn()
  }

  @CommandHandler(CommandName.JoinAsSpectator)
  suspend fun joinAsSpectator(socket: UserSocket) {
    // TODO(Assasans): Shit
    val resourcesMap1Reader = Paths.get("src/main/resources/resources/maps/sandbox-summer-1.json").absolute().bufferedReader()
    val resourcesMap1 = resourcesMap1Reader.use { it.readText() }

    // TODO(Assasans): Shit
    val resourcesMap2Reader = Paths.get("src/main/resources/resources/maps/sandbox-summer-2.json").absolute().bufferedReader()
    val resourcesMap2 = resourcesMap2Reader.use { it.readText() }

    // TODO(Assasans): Shit
    val resourcesMap3Reader = Paths.get("src/main/resources/resources/maps/sandbox-summer-3.json").absolute().bufferedReader()
    val resourcesMap3 = resourcesMap3Reader.use { it.readText() }

    val player = BattlePlayer(
      socket = socket,
      battle = battleProcessor.battles[0],
      team = BattleTeam.None,
      isSpectator = true
    )
    battleProcessor.battles[0].players.add(player)

    // BattlePlayer(socket, this, null)

    socket.initBattleLoad()

    socket.awaitDependency(socket.loadDependency(resourcesMap1))
    socket.awaitDependency(socket.loadDependency(resourcesMap2))
    socket.awaitDependency(socket.loadDependency(resourcesMap3))

    player.init()
  }

  @CommandHandler(CommandName.InitSpectatorUser)
  suspend fun initSpectatorUser(socket: UserSocket) {
    // TODO(Assasans): Shit
    val shotsDataReader = Paths.get("src/main/resources/resources/shots-data.json").absolute().bufferedReader()
    val shotsData = shotsDataReader.use { it.readText() }

    Command(CommandName.InitShotsData, mutableListOf(shotsData)).send(socket) // TODO(Assasans): initBattleLoad?

    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val battle = player.battle

    player.initLocal()
  }

  @CommandHandler(CommandName.SwitchBattleSelect)
  suspend fun switchBattleSelect(socket: UserSocket) {
    logger.debug { "Switch to battle select" }

    val player = socket.battlePlayer
    if(player != null) {
      Command(CommandName.UnloadBattle).send(socket)
    }
  }

  @CommandHandler(CommandName.ExitFromBattleNotify)
  suspend fun exitFromBattleNotify(socket: UserSocket) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val battle = player.battle

    battle.players.remove(player)

    Command(
      CommandName.InitMessages,
      listOf(
        InitChatMessagesData(
          messages = listOf(
            ChatMessage(name = "roflanebalo", rang = 4, message = "Ты пидорас")
          )
        ).toJson(),
        InitChatSettings(
          selfName = socket.user!!.username
        ).toJson()
      )
    ).send(socket)

    socket.initBattleList()

    logger.debug { "Select battle ${battle.id} -> ${battle.title}" }

    battle.selectFor(socket)
    battle.showInfoFor(socket)
  }
}
