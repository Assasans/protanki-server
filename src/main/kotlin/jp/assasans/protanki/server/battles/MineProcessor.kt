package jp.assasans.protanki.server.battles

import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName

interface IMineProcessor {
  val battle: Battle
  val mines: MutableMap<Int, BattleMine>

  fun incrementId()
  suspend fun spawn(mine: BattleMine)
  suspend fun deactivateAll(player: BattlePlayer, native: Boolean = true)
}

class MineProcessor(
  override val battle: Battle
) : IMineProcessor {
  override val mines: MutableMap<Int, BattleMine> = mutableMapOf()

  var nextId: Int = 0
    private set

  override fun incrementId() {
    nextId++
  }

  override suspend fun spawn(mine: BattleMine) {
    mines[mine.id] = mine
    mine.spawn()
  }

  override suspend fun deactivateAll(player: BattlePlayer, native: Boolean) {
    if(native) {
      if(mines.values.none { mine -> mine.owner == player }) return
      Command(CommandName.RemoveMines, player.user.username).sendTo(battle)
      mines.values.removeAll { mine -> mine.owner == player }
    } else {
      mines.values
        .filter { mine -> mine.owner == player }
        .forEach { mine -> mine.deactivate() }
    }
  }
}
