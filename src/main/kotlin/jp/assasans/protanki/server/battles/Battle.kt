package jp.assasans.protanki.server.battles

import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.random.nextULong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import mu.KotlinLogging
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import jp.assasans.protanki.server.ServerMapInfo
import jp.assasans.protanki.server.battles.bonus.BonusProcessor
import jp.assasans.protanki.server.battles.mode.BattleModeHandler
import jp.assasans.protanki.server.battles.mode.BattleModeHandlerBuilder
import jp.assasans.protanki.server.battles.mode.DeathmatchModeHandler
import jp.assasans.protanki.server.battles.mode.TeamModeHandler
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName

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

enum class BattleMode(val key: String, val id: Int) {
  Deathmatch("DM", 1),
  TeamDeathmatch("TDM", 2),
  CaptureTheFlag("CTF", 3),
  ControlPoints("CP", 4);

  companion object {
    private val map = values().associateBy(BattleMode::key)
    private val mapById = values().associateBy(BattleMode::id)

    fun get(key: String) = map[key]
    fun getById(id: Int) = mapById[id]
  }
}

enum class SendTarget {
  Players,
  Spectators
}

suspend fun Command.sendTo(
  battle: Battle,
  vararg targets: SendTarget = arrayOf(SendTarget.Players, SendTarget.Spectators),
  exclude: BattlePlayer? = null
): Int = battle.sendTo(this, *targets, exclude = exclude)

fun List<BattlePlayer>.users() = filter { player -> !player.isSpectator }
fun List<BattlePlayer>.spectators() = filter { player -> player.isSpectator }

class Battle(
  coroutineContext: CoroutineContext,
  val id: String,
  val title: String,
  var map: ServerMapInfo,
  var fund: Int = 1337228,
  modeHandlerBuilder: BattleModeHandlerBuilder
) {
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
      is DeathmatchModeHandler -> DmBattleData(
        battleId = id,
        battleMode = modeHandler.mode,
        map = map.name,
        name = title,
        maxPeople = 8,
        minRank = 1,
        maxRank = 30,
        preview = map.preview,
        parkourMode = properties[BattleProperty.ParkourMode],
        users = players.users().map { player -> player.user.username },
      )
      is TeamModeHandler       -> TeamBattleData(
        battleId = id,
        battleMode = modeHandler.mode,
        map = map.name,
        name = title,
        maxPeople = 8,
        minRank = 1,
        maxRank = 30,
        preview = map.preview,
        parkourMode = properties[BattleProperty.ParkourMode],
        usersRed = players
          .users()
          .filter { player -> player.team == BattleTeam.Red }
          .map { player -> player.user.username },
        usersBlue = players
          .users()
          .filter { player -> player.team == BattleTeam.Blue }
          .map { player -> player.user.username }
      )
      else                     -> throw IllegalStateException("Unknown battle mode: ${modeHandler::class}")
    }
  }

  suspend fun selectFor(socket: UserSocket) {
    Command(CommandName.ClientSelectBattle, id).send(socket)
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
        reArmorEnabled = true,
        parkourMode = properties[BattleProperty.ParkourMode],
        users = players.users().map { player -> BattleUser(user = player.user.username, kills = player.kills, score = player.score) },
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
        reArmorEnabled = true,
        parkourMode = properties[BattleProperty.ParkourMode],
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

    Command(CommandName.ShowBattleInfo, info).send(socket)
  }

  suspend fun restart() {
    val restartTime = 10.seconds

    Command(
      CommandName.FinishBattle,
      FinishBattleData(
        time_to_restart = restartTime.inWholeMilliseconds,
        users = players.users().map { player ->
          FinishBattleUserData(
            username = player.user.username,
            rank = player.user.rank.value.value,
            team = player.team,
            score = player.score,
            kills = player.kills,
            deaths = player.deaths,
            prize = 21,
            bonus_prize = 12
          )
        }
      ).toJson()
    ).sendTo(this)
    logger.debug { "Finished battle $id" }

    delay(restartTime.inWholeMilliseconds)

    players.users().forEach { player -> player.respawn() }
    Command(CommandName.RestartBattle, 0.toString()).sendTo(this)

    logger.debug { "Restarted battle $id" }
  }

  suspend fun sendTo(
    command: Command,
    vararg targets: SendTarget = arrayOf(SendTarget.Players, SendTarget.Spectators),
    exclude: BattlePlayer? = null
  ): Int {
    var count = 0
    if(targets.contains(SendTarget.Players)) {
      players
        .users()
        .filter { player -> player.socket.active }
        .filter { player -> exclude == null || player != exclude }
        .filter { player -> player.ready }
        .forEach { player ->
          command.send(player)
          count++
        }
    }
    if(targets.contains(SendTarget.Spectators)) {
      players
        .spectators()
        .filter { player -> player.socket.active }
        .filter { player -> exclude == null || player != exclude }
        .filter { player -> player.ready }
        .forEach { player ->
          command.send(player)
          count++
        }
    }

    return count
  }
}

interface IBattleProcessor {
  val battles: MutableList<Battle>

  fun getBattle(id: String): Battle?
}

class BattleProcessor : IBattleProcessor {
  private val logger = KotlinLogging.logger { }

  override val battles: MutableList<Battle> = mutableListOf()

  override fun getBattle(id: String): Battle? = battles.singleOrNull { battle -> battle.id == id }
}
