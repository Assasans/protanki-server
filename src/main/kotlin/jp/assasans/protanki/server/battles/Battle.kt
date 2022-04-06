package jp.assasans.protanki.server.battles

import kotlin.random.Random
import kotlin.random.nextULong
import mu.KotlinLogging
import jp.assasans.protanki.server.ServerMapInfo
import jp.assasans.protanki.server.battles.mode.BattleModeHandler
import jp.assasans.protanki.server.battles.mode.BattleModeHandlerBuilder
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.client.railgun.FireTarget
import jp.assasans.protanki.server.client.railgun.ShotTarget
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.garage.ServerGarageUserItemWeapon

interface ITickHandler {
  suspend fun tick() {}
}

enum class TankState {
  Dead,
  Respawn,
  SemiActive,
  Active
}

abstract class WeaponHandler(
  val player: BattlePlayer,
  val item: ServerGarageUserItemWeapon
) {
}

class RailgunWeaponHandler(
  player: BattlePlayer,
  weapon: ServerGarageUserItemWeapon
) : WeaponHandler(player, weapon) {
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
  exclude: BattlePlayer? = null
): Int = battle.sendTo(this, *targets, exclude = exclude)

fun List<BattlePlayer>.users() = filter { player -> !player.isSpectator }
fun List<BattlePlayer>.spectators() = filter { player -> player.isSpectator }

class Battle(
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

  val modeHandler: BattleModeHandler = modeHandlerBuilder(this)
  val players: MutableList<BattlePlayer> = mutableListOf()

  fun toBattleData(): BattleData {
    // TODO(Assasans)
    return BattleData(
      battleId = id,
      battleMode = modeHandler.mode,
      map = map.name,
      name = title,
      maxPeople = 8,
      minRank = 0,
      maxRank = 30,
      preview = map.preview,
      users = listOf()
    )
  }

  suspend fun selectFor(socket: UserSocket) {
    Command(
      CommandName.ClientSelectBattle,
      listOf(id)
    ).send(socket)
  }

  suspend fun showInfoFor(socket: UserSocket) {
    Command(
      CommandName.ShowBattleInfo,
      listOf(
        ShowDmBattleInfoData(
          itemId = id,
          battleMode = modeHandler.mode,
          scoreLimit = 300,
          timeLimitInSec = 600,
          timeLeftInSec = 212,
          preview = map.preview,
          maxPeopleCount = 8,
          name = title,
          minRank = 0,
          maxRank = 30,
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
    vararg targets: SendTarget = arrayOf(SendTarget.Players, SendTarget.Spectators),
    exclude: BattlePlayer? = null
  ): Int {
    var count = 0
    if(targets.contains(SendTarget.Players)) {
      players
        .users()
        .filter { player -> exclude == null || player != exclude }
        .forEach { player ->
          command.send(player)
          count++
        }
    }
    if(targets.contains(SendTarget.Spectators)) {
      players
        .spectators()
        .filter { player -> exclude == null || player != exclude }
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
