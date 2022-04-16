package jp.assasans.protanki.server.battles

import jp.assasans.protanki.server.client.railgun.FireTarget
import jp.assasans.protanki.server.client.toJson
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.garage.ServerGarageUserItemWeapon

class RailgunWeaponHandler(
  player: BattlePlayer,
  weapon: ServerGarageUserItemWeapon
) : WeaponHandler(player, weapon) {
  suspend fun fireStart() {
    val tank = player.tank ?: throw Exception("No Tank")

    Command(CommandName.StartFire, listOf(tank.id)).sendTo(tank.player.battle)
  }

  suspend fun fireTarget(target: FireTarget) {
    val tank = player.tank ?: throw Exception("No Tank")

    Command(CommandName.ShotTarget, listOf(tank.id, target.toJson())).sendTo(tank.player.battle)
  }
}
