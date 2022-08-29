package jp.assasans.protanki.server.battles.weapons

import jp.assasans.protanki.server.battles.*
import jp.assasans.protanki.server.client.send
import jp.assasans.protanki.server.client.weapons.flamethrower.FireTarget
import jp.assasans.protanki.server.client.weapons.flamethrower.StartFire
import jp.assasans.protanki.server.client.weapons.flamethrower.StopFire
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.garage.ServerGarageUserItemWeapon

class FlamethrowerWeaponHandler(
  player: BattlePlayer,
  weapon: ServerGarageUserItemWeapon
) : WeaponHandler(player, weapon) {
  private var fireStarted = false

  suspend fun fireStart(startFire: StartFire) {
    val tank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    fireStarted = true

    Command(CommandName.ClientStartFire, tank.id).send(battle.players.exclude(player).ready())
  }

  suspend fun fireTarget(target: FireTarget) {
    val sourceTank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    // TODO(Assasans): Damage timing is not checked on server, exploitation is possible
    if(!fireStarted) return

    val targetTanks = battle.players
      .mapNotNull { player -> player.tank }
      .filter { tank -> target.targets.contains(tank.id) }
      .filter { tank -> tank.state == TankState.Active }

    targetTanks.forEach { targetTank ->
      val damage = damageCalculator.calculate(sourceTank, targetTank)
      battle.damageProcessor.dealDamage(sourceTank, targetTank, damage.damage, damage.isCritical)
    }

    // TODO(Assasans): No response command?
  }

  suspend fun fireStop(stopFire: StopFire) {
    val tank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    fireStarted = false

    Command(CommandName.ClientStopFire, tank.id).send(battle.players.exclude(player).ready())
  }
}
