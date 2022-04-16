package jp.assasans.protanki.server.battles

import jp.assasans.protanki.server.garage.ServerGarageUserItemWeapon

class NullWeaponHandler(
  player: BattlePlayer,
  weapon: ServerGarageUserItemWeapon
) : WeaponHandler(player, weapon) {
}
