package jp.assasans.protanki.server.battles.weapons

import jp.assasans.protanki.server.battles.BattlePlayer
import jp.assasans.protanki.server.garage.ServerGarageUserItemWeapon

abstract class WeaponHandler(
  val player: BattlePlayer,
  val item: ServerGarageUserItemWeapon
) {
}
