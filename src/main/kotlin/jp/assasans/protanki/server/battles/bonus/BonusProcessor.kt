package jp.assasans.protanki.server.battles.bonus

import jp.assasans.protanki.server.battles.Battle

interface IBonusProcessor {
  val battle: Battle
  val bonuses: MutableMap<Int, BattleBonus>

  fun incrementId()
  suspend fun spawn(bonus: BattleBonus)
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
}
