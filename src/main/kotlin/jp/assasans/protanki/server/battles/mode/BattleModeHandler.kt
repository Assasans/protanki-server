package jp.assasans.protanki.server.battles.mode

import jp.assasans.protanki.server.battles.Battle
import jp.assasans.protanki.server.battles.BattleMode
import jp.assasans.protanki.server.battles.BattlePlayer

typealias BattleModeHandlerBuilder = (battle: Battle) -> BattleModeHandler

abstract class BattleModeHandler(
  val battle: Battle
) {
  abstract val mode: BattleMode

  abstract suspend fun playerJoin(player: BattlePlayer)
  abstract suspend fun playerLeave(player: BattlePlayer)
  abstract suspend fun initModeModel(player: BattlePlayer)
  open suspend fun initPostGui(player: BattlePlayer) {}

  open suspend fun dump(builder: StringBuilder) {}
}
