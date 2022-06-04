package jp.assasans.protanki.server.battles

import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.random.nextULong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import mu.KotlinLogging
import jp.assasans.protanki.server.ServerMapInfo
import jp.assasans.protanki.server.battles.bonus.BonusProcessor
import jp.assasans.protanki.server.battles.mode.BattleModeHandler
import jp.assasans.protanki.server.battles.mode.BattleModeHandlerBuilder
import jp.assasans.protanki.server.battles.mode.DeathmatchModeHandler
import jp.assasans.protanki.server.battles.mode.TeamModeHandler
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName

interface ITickHandler {
  suspend fun tick() {}
}

enum class TankState {
  Dead,
  Respawn,
  SemiActive,
  Active
}

val TankState.tankInitKey: String
  get() = when(this) {
    TankState.Dead       -> "suicide"
    TankState.Respawn    -> "suicide"
    TankState.SemiActive -> "newcome"
    TankState.Active     -> "active"
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

val BattleTeam.opposite: BattleTeam
  get() {
    return when(this) {
      BattleTeam.None -> BattleTeam.None
      BattleTeam.Red  -> BattleTeam.Blue
      BattleTeam.Blue -> BattleTeam.Red
    }
  }

enum class BattleMode(val key: String) {
  Deathmatch("DM"),
  TeamDeathmatch("TDM"),
  CaptureTheFlag("CTF"),
  ControlPoints("CP");

  companion object {
    private val map = values().associateBy(BattleMode::key)

    fun get(key: String) = map[key]
  }
}

enum class SendTarget {
  Players,
  Spectators
}

suspend fun Command.sendTo(
  battle: Battle,
  vararg targets: SendTarget = arrayOf(SendTarget.Players, SendTarget.Spectators),
  exclude: BattlePlayer? = null,
  minimumLoadState: LoadState = LoadState.Stage1
): Int = battle.sendTo(this, *targets, exclude = exclude, minimumLoadState = minimumLoadState)

fun List<BattlePlayer>.users() = filter { player -> !player.isSpectator }
fun List<BattlePlayer>.spectators() = filter { player -> player.isSpectator }

class Battle(
  coroutineContext: CoroutineContext,
  val id: String,
  val title: String,
  var map: ServerMapInfo,
  var fund: Int = 1337228,
  modeHandlerBuilder: BattleModeHandlerBuilder
) : ITickHandler {
  companion object {
    fun generateId(): String = Random.nextULong().toString(16)
  }

  private val logger = KotlinLogging.logger { }

  val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())

  val properties: BattleProperties = BattleProperties()
  val modeHandler: BattleModeHandler = modeHandlerBuilder(this)
  val players: MutableList<BattlePlayer> = mutableListOf()

  val damageProcessor = DamageProcessor(this)
  val bonusProcessor = BonusProcessor(this)
  val mineProcessor = MineProcessor(this)

  fun toBattleData(): BattleData {
    // TODO(Assasans)
    return when(modeHandler) {
      is DeathmatchModeHandler     -> DmBattleData(
        battleId = id,
        battleMode = modeHandler.mode,
        map = map.name,
        name = title,
        maxPeople = 8,
        minRank = 1,
        maxRank = 30,
        preview = map.preview,
        users = players.users().map { player -> player.user.username }
      )
      is TeamModeHandler -> TeamBattleData(
        battleId = id,
        battleMode = modeHandler.mode,
        map = map.name,
        name = title,
        maxPeople = 8,
        minRank = 1,
        maxRank = 30,
        preview = map.preview,
        usersRed = players
          .users()
          .filter { player -> player.team == BattleTeam.Red }
          .map { player -> player.user.username },
        usersBlue = players
          .users()
          .filter { player -> player.team == BattleTeam.Blue }
          .map { player -> player.user.username }
      )
      else                         -> throw IllegalStateException("Unknown battle mode: ${modeHandler::class}")
    }
  }

  suspend fun selectFor(socket: UserSocket) {
    Command(
      CommandName.ClientSelectBattle,
      listOf(id)
    ).send(socket)
  }

  suspend fun showInfoFor(socket: UserSocket) {
    val info = when(modeHandler) {
      is DeathmatchModeHandler -> ShowDmBattleInfoData(
        itemId = id,
        battleMode = modeHandler.mode,
        scoreLimit = 300,
        timeLimitInSec = 600,
        timeLeftInSec = 212,
        preview = map.preview,
        maxPeopleCount = 8,
        name = title,
        minRank = 1,
        maxRank = 30,
        spectator = true,
        withoutBonuses = false,
        withoutCrystals = false,
        withoutSupplies = false,
        users = players.users().map { player -> BattleUser(user = player.user.username, kills = player.kills, score = player.score) },
        score = 123
      ).toJson()
      is TeamModeHandler       -> ShowTeamBattleInfoData(
        itemId = id,
        battleMode = modeHandler.mode,
        scoreLimit = 300,
        timeLimitInSec = 600,
        timeLeftInSec = 212,
        preview = map.preview,
        maxPeopleCount = 8,
        name = title,
        minRank = 1,
        maxRank = 30,
        spectator = true,
        withoutBonuses = false,
        withoutCrystals = false,
        withoutSupplies = false,
        usersRed = players
          .users()
          .filter { player -> player.team == BattleTeam.Red }
          .map { player -> BattleUser(user = player.user.username, kills = player.kills, score = player.score) },
        usersBlue = players
          .users()
          .filter { player -> player.team == BattleTeam.Blue }
          .map { player -> BattleUser(user = player.user.username, kills = player.kills, score = player.score) },
        scoreRed = modeHandler.teamScores[BattleTeam.Red] ?: 0,
        scoreBlue = modeHandler.teamScores[BattleTeam.Blue] ?: 0,
        autoBalance = false,
        friendlyFire = properties[BattleProperty.FriendlyFireEnabled]
      ).toJson()
      else                     -> throw IllegalStateException("Unknown battle mode: ${modeHandler.mode}")
    }

    Command(
      CommandName.ShowBattleInfo,
      listOf(info)
    ).send(socket)
  }

  suspend fun sendTo(
    command: Command,
    vararg targets: SendTarget = arrayOf(SendTarget.Players, SendTarget.Spectators),
    exclude: BattlePlayer? = null,
    minimumLoadState: LoadState = LoadState.Stage1
  ): Int {
    var count = 0
    if(targets.contains(SendTarget.Players)) {
      players
        .users()
        .filter { player -> exclude == null || player != exclude }
        .filter { player -> player.loadState >= minimumLoadState }
        .forEach { player ->
          command.send(player)
          count++
        }
    }
    if(targets.contains(SendTarget.Spectators)) {
      players
        .spectators()
        .filter { player -> exclude == null || player != exclude }
        .filter { player -> player.loadState >= minimumLoadState }
        .forEach { player ->
          command.send(player)
          count++
        }
    }

    return count
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
