package jp.assasans.protanki.server.battles.bonus

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import jp.assasans.protanki.server.BonusType
import jp.assasans.protanki.server.battles.Battle
import jp.assasans.protanki.server.battles.BattleTank
import jp.assasans.protanki.server.battles.sendTo
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.math.Quaternion
import jp.assasans.protanki.server.math.Vector3

class BattleGoldBonus(battle: Battle, id: Int, position: Vector3, rotation: Quaternion) :
  BattleBonus(battle, id, position, rotation, 10.minutes) {
  override val type: BonusType = BonusType.Gold

  override suspend fun spawn() {
    Command(CommandName.SpawnGold, "Скоро будет сброшен золотой ящик", 490113.toString()).sendTo(battle)
    delay(20.seconds.inWholeMilliseconds)
    super.spawn()
  }

  override suspend fun activate(tank: BattleTank) {
    tank.player.user.crystals += 1000
    tank.socket.updateCrystals()

    Command(CommandName.TakeGold, tank.id).sendTo(battle)
  }
}
