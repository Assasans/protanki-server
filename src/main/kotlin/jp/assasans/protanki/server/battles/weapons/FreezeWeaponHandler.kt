package jp.assasans.protanki.server.battles.weapons

import jp.assasans.protanki.server.battles.BattlePlayer
import jp.assasans.protanki.server.battles.TankState
import jp.assasans.protanki.server.battles.sendTo
import jp.assasans.protanki.server.client.flamethrower.FireTarget
import jp.assasans.protanki.server.client.flamethrower.StartFire
import jp.assasans.protanki.server.client.flamethrower.StopFire
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.garage.ServerGarageUserItemWeapon

class FreezeWeaponHandler(
  player: BattlePlayer,
  weapon: ServerGarageUserItemWeapon
) : WeaponHandler(player, weapon) {
  private var fireStarted = false

  suspend fun fireStart(startFire: StartFire) {
    val tank = player.tank ?: throw Exception("No Tank")

    fireStarted = true

    Command(CommandName.ClientStartFire, listOf(tank.id)).sendTo(tank.player.battle)
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
      battle.damageProcessor.dealDamage(sourceTank, targetTank, 20.0, isCritical = false)
    }
  }

  suspend fun fireStop(stopFire: StopFire) {
    val tank = player.tank ?: throw Exception("No Tank")

    fireStarted = false

    Command(CommandName.ClientStopFire, listOf(tank.id)).sendTo(tank.player.battle)
  }
}
