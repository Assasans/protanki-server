package jp.assasans.protanki.server.battles

import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import jp.assasans.protanki.server.client.AddMineData
import jp.assasans.protanki.server.client.toJson
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.extensions.launchDelayed
import jp.assasans.protanki.server.math.Vector3

class BattleMine(
  val id: Int,
  val owner: BattlePlayer,
  val position: Vector3
) {
  var spawnTime: Instant? = null
    private set

  val battle: Battle
    get() = owner.battle

  val key: String
    get() = "${owner.user.username}_$id"

  suspend fun spawn() {
    spawnTime = Clock.System.now()

    Command(CommandName.AddMine, toAddMine().toJson()).sendTo(battle)

    owner.coroutineScope.launchDelayed(1.seconds) {
      Command(CommandName.ActivateMine, key).sendTo(battle)
    }
  }

  suspend fun trigger(tank: BattleTank) {
    // Theoretically possible only before the first tank spawn.
    // BattlePlayer.tank is never assigned null after the first spawn.
    val sourceTank = owner.tank ?: throw IllegalStateException("Owner tank is null")

    if(tank.state != TankState.Active) return

    battle.damageProcessor.dealDamage(sourceTank, tank, 80.0, false, ignoreSourceEffects = true)

    Command(CommandName.ClientTriggerMine, key, tank.player.user.username).sendTo(battle)
    battle.mineProcessor.mines.remove(id)
  }

  suspend fun deactivate() {
    Command(CommandName.ClientTriggerMine, key, "").sendTo(battle)
    battle.mineProcessor.mines.remove(id)
  }
}

fun BattleMine.toAddMine() = AddMineData(
  mineId = key,
  userId = owner.user.username,
  x = position.x,
  y = position.y,
  z = position.z
)
