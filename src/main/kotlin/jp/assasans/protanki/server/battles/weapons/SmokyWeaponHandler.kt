package jp.assasans.protanki.server.battles.weapons

import jp.assasans.protanki.server.battles.BattlePlayer
import jp.assasans.protanki.server.battles.TankState
import jp.assasans.protanki.server.battles.calculate
import jp.assasans.protanki.server.battles.sendTo
import jp.assasans.protanki.server.client.smoky.Fire
import jp.assasans.protanki.server.client.smoky.FireStatic
import jp.assasans.protanki.server.client.smoky.FireTarget
import jp.assasans.protanki.server.client.smoky.ShotTarget
import jp.assasans.protanki.server.client.toJson
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.garage.ServerGarageUserItemWeapon

class SmokyWeaponHandler(
  player: BattlePlayer,
  weapon: ServerGarageUserItemWeapon
) : WeaponHandler(player, weapon) {
  suspend fun fire(fire: Fire) {
    val tank = player.tank ?: throw Exception("No Tank")

    Command(CommandName.Shot, tank.id, fire.toJson()).sendTo(tank.player.battle)
  }

  suspend fun fireStatic(static: FireStatic) {
    val tank = player.tank ?: throw Exception("No Tank")

    Command(CommandName.ShotStatic, tank.id, static.toJson()).sendTo(tank.player.battle)
  }

  suspend fun fireTarget(target: FireTarget) {
    val sourceTank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    val targetTank = battle.players
      .mapNotNull { player -> player.tank }
      .single { tank -> tank.id == target.target }
    if(targetTank.state != TankState.Active) return

    val damage = damageCalculator.calculate(sourceTank, targetTank)
    battle.damageProcessor.dealDamage(sourceTank, targetTank, damage.damage, damage.isCritical)

    val shot = ShotTarget(target, damage.weakening, false)
    Command(CommandName.ShotTarget, sourceTank.id, shot.toJson()).sendTo(sourceTank.player.battle)
  }
}
