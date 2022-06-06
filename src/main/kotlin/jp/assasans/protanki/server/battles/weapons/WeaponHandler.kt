package jp.assasans.protanki.server.battles.weapons

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.battles.BattlePlayer
import jp.assasans.protanki.server.battles.IDamageCalculator
import jp.assasans.protanki.server.garage.ServerGarageUserItemWeapon

abstract class WeaponHandler(
  val player: BattlePlayer,
  val item: ServerGarageUserItemWeapon
) : KoinComponent {
  protected val damageCalculator: IDamageCalculator by inject()
}
