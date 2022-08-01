package jp.assasans.protanki.server.battles.effect

import kotlin.time.Duration.Companion.seconds
import jp.assasans.protanki.server.battles.BattleTank

class RepairKitEffect(
  tank: BattleTank
) : TankEffect(
  tank,
  duration = 2.seconds,
  cooldown = 20.seconds
) {
  override val info: EffectInfo
    get() = EffectInfo(
      id = 1,
      name = "health"
    )

  override suspend fun activate() {
    val battle = tank.battle
    val damageProcessor = battle.damageProcessor

    damageProcessor.heal(tank, 150.0) // TODO(Assasans)
  }
}
