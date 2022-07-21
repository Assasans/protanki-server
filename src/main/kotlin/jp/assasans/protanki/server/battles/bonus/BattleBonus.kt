package jp.assasans.protanki.server.battles.bonus

import kotlin.time.Duration
import kotlinx.coroutines.Job
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import jp.assasans.protanki.server.BonusType
import jp.assasans.protanki.server.battles.Battle
import jp.assasans.protanki.server.battles.BattleTank
import jp.assasans.protanki.server.battles.sendTo
import jp.assasans.protanki.server.client.toJson
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.extensions.launchDelayed
import jp.assasans.protanki.server.math.Quaternion
import jp.assasans.protanki.server.math.Vector3

abstract class BattleBonus(
  val battle: Battle,
  val id: Int,
  val position: Vector3,
  val rotation: Quaternion,
  val lifetime: Duration
) {
  abstract val type: BonusType

  var spawnTime: Instant? = null
    protected set

  val aliveFor: Duration
    get() {
      val spawnTime = spawnTime ?: throw IllegalStateException("Bonus is not spawned")
      return Clock.System.now() - spawnTime
    }

  var removeJob: Job? = null
    protected set

  val key: String
    get() = "${type.bonusKey}_$id"

  fun cancelRemove() {
    removeJob?.cancel()
    removeJob = null
  }

  open suspend fun spawn() {
    Command(
      CommandName.SpawnBonus,
      SpawnBonusDatta(
        id = key,
        x = position.x,
        y = position.y,
        z = position.z,
        disappearing_time = lifetime.inWholeSeconds.toInt()
      ).toJson()
    ).sendTo(battle)

    spawnTime = Clock.System.now()
    removeJob = battle.coroutineScope.launchDelayed(lifetime) {
      battle.bonusProcessor.bonuses.remove(id)

      Command(CommandName.RemoveBonus, key).sendTo(battle)
    }
  }

  abstract suspend fun activate(tank: BattleTank)
}
