package jp.assasans.protanki.server.battles.weapons

import jp.assasans.protanki.server.battles.*
import jp.assasans.protanki.server.client.send
import jp.assasans.protanki.server.client.weapons.shaft.FireTarget
import jp.assasans.protanki.server.client.weapons.shaft.ShotTarget
import jp.assasans.protanki.server.client.toJson
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.garage.ServerGarageUserItemWeapon

class ShaftWeaponHandler(
  player: BattlePlayer,
  weapon: ServerGarageUserItemWeapon
) : WeaponHandler(player, weapon) {
  suspend fun startEnergyDrain(time: Int) {
    val tank = player.tank ?: throw Exception("No Tank")

    // TODO(Assasans)
  }

  suspend fun enterSnipingMode() {
    val tank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    Command(CommandName.ClientEnterSnipingMode, tank.id).send(battle.players.exclude(player).ready())
  }

  suspend fun exitSnipingMode() {
    val tank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    Command(CommandName.ClientExitSnipingMode, tank.id).send(battle.players.exclude(player).ready())
  }

  suspend fun fireArcade(target: FireTarget) {
    val sourceTank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    if(target.target != null) {
      val targetTank = battle.players
        .mapNotNull { player -> player.tank }
        .single { tank -> tank.id == target.target }
      if(targetTank.state != TankState.Active) return

      battle.damageProcessor.dealDamage(sourceTank, targetTank, 75.0, false)
    }

    val shot = ShotTarget(target, 5.0)
    Command(CommandName.ShotTarget, sourceTank.id, shot.toJson()).send(battle.players.exclude(player).ready())
  }

  suspend fun fireSniping(target: FireTarget) {
    val sourceTank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    if(target.target != null) {
      val targetTank = battle.players
        .mapNotNull { player -> player.tank }
        .single { tank -> tank.id == target.target }
      if(targetTank.state != TankState.Active) return

      battle.damageProcessor.dealDamage(sourceTank, targetTank, 150.0, false)
    }

    val shot = ShotTarget(target, 5.0)
    Command(CommandName.ShotTarget, sourceTank.id, shot.toJson()).send(battle.players.exclude(player).ready())
  }
}
