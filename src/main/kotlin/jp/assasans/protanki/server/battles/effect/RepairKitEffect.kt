package jp.assasans.protanki.server.battles.effect

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import jp.assasans.protanki.server.battles.BattleTank

class RepairKitEffect(
  tank: BattleTank
) : TankEffect(
  tank,
  duration = 3.seconds,
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

    // TODO(Assasans): More complicated logic
    damageProcessor.heal(tank, 1500.0)

    if(duration == null) return
    tank.coroutineScope.launch {
      val startTime = Clock.System.now()
      val endTime = startTime + duration
      while(Clock.System.now() < endTime) {
        delay(500)
        if(tank.health < tank.maxHealth) {
          damageProcessor.heal(tank, 300.0)
        }
      }
    }
  }
}
