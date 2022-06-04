package jp.assasans.protanki.server.battles.weapons

import jp.assasans.protanki.server.battles.BattlePlayer
import jp.assasans.protanki.server.garage.ServerGarageUserItemWeapon

class NullWeaponHandler(
  player: BattlePlayer,
  weapon: ServerGarageUserItemWeapon
) : WeaponHandler(player, weapon) {
}
