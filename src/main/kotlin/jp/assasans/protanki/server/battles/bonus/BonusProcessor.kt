package jp.assasans.protanki.server.battles.bonus

import jp.assasans.protanki.server.battles.Battle
import jp.assasans.protanki.server.battles.BattleTank
import jp.assasans.protanki.server.battles.sendTo
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName

interface IBonusProcessor {
  val battle: Battle
  val bonuses: MutableMap<Int, BattleBonus>

  fun incrementId()
  suspend fun spawn(bonus: BattleBonus)
  suspend fun activate(bonus: BattleBonus, tank: BattleTank)
}

class BonusProcessor(
  override val battle: Battle
) : IBonusProcessor {
  override val bonuses: MutableMap<Int, BattleBonus> = mutableMapOf()

  var nextId: Int = 0
    private set

  override fun incrementId() {
    nextId++
  }

  override suspend fun spawn(bonus: BattleBonus) {
    bonuses[bonus.id] = bonus
    bonus.spawn()
  }

  override suspend fun activate(bonus: BattleBonus, tank: BattleTank) {
    bonus.cancelRemove()
    bonus.activate(tank)
    Command(CommandName.ActivateBonus, listOf(bonus.key)).sendTo(battle)

    bonuses.remove(bonus.id)
  }
}
