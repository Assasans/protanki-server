package jp.assasans.protanki.server.battles.effect

import kotlin.time.Duration
import mu.KotlinLogging
import jp.assasans.protanki.server.battles.BattleTank
import jp.assasans.protanki.server.battles.sendTo
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.extensions.launchDelayed

abstract class TankEffect(
  val tank: BattleTank,
  val duration: Duration?,
  val cooldown: Duration?
) {
  data class EffectInfo(
    val id: Int,
    val name: String
  )

  private val logger = KotlinLogging.logger { }

  abstract val info: EffectInfo

  var isActive: Boolean = false
    private set

  suspend fun run() {
    activate()
    Command(
      CommandName.EnableEffect,
      tank.id,
      info.id.toString(),
      (duration?.inWholeMilliseconds ?: 0).toString(),
      false.toString(), // Active after respawn
      0.toString() // Effect level
    ).sendTo(tank.battle)

    logger.debug { "Activated effect ${this::class.simpleName} for tank ${tank.id} (player: ${tank.player.user.username})" }

    if(duration != null) {
      tank.coroutineScope.launchDelayed(duration) {
        deactivate()
        Command(
          CommandName.DisableEffect,
          tank.id,
          info.id.toString(),
          false.toString() // Active after respawn
        ).sendTo(tank.battle)

        logger.debug { "Deactivated effect ${this@TankEffect::class.simpleName} for tank ${tank.id} (player: ${tank.player.user.username})" }

        tank.effects.remove(this@TankEffect)
      }
    } else {
      tank.effects.remove(this)
    }
  }

  open suspend fun activate() {}
  open suspend fun deactivate() {}
}
