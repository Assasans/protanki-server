package jp.assasans.protanki.server.battles.weapons

import kotlin.math.ceil
import jp.assasans.protanki.server.battles.BattlePlayer
import jp.assasans.protanki.server.battles.TankState
import jp.assasans.protanki.server.battles.sendTo
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
    val sourceTank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    // Preserve order of targets
    // TODO(Assasans): Replace with a more efficient algorithm
    val targetTanks = target.targets
      .mapNotNull { username -> battle.players.singleOrNull { player -> player.user.username == username } }
      .mapNotNull { player -> player.tank }
      .filter { tank -> target.targets.contains(tank.id) }
      .filter { tank -> tank.state == TankState.Active }

    var damage = 70.0
    targetTanks.forEach { targetTank ->
      battle.damageProcessor.dealDamage(sourceTank, targetTank, damage, isCritical = false)
      damage = ceil(damage * 0.5)
    }

    Command(CommandName.ShotTarget, listOf(sourceTank.id, target.toJson())).sendTo(player.battle)
  }
}
