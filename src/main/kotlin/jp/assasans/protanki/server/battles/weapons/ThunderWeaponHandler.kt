package jp.assasans.protanki.server.battles.weapons

import jp.assasans.protanki.server.battles.*
import jp.assasans.protanki.server.client.send
import jp.assasans.protanki.server.client.weapons.thunder.Fire
import jp.assasans.protanki.server.client.weapons.thunder.FireStatic
import jp.assasans.protanki.server.client.weapons.thunder.FireTarget
import jp.assasans.protanki.server.client.toJson
import jp.assasans.protanki.server.client.toVector
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.garage.ServerGarageUserItemWeapon
import jp.assasans.protanki.server.math.Vector3
import jp.assasans.protanki.server.math.Vector3Constants
import jp.assasans.protanki.server.math.distanceTo

class ThunderWeaponHandler(
  player: BattlePlayer,
  weapon: ServerGarageUserItemWeapon
) : WeaponHandler(player, weapon) {
  suspend fun fire(fire: Fire) {
    val tank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    Command(CommandName.Shot, tank.id, fire.toJson()).send(battle.players.exclude(player).ready())
  }

  suspend fun fireStatic(static: FireStatic) {
    val tank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    processSplashTargets(static.hitPoint.toVector(), static.splashTargetIds, static.splashTargetDistances)

    Command(CommandName.ShotStatic, tank.id, static.toJson()).send(battle.players.exclude(player).ready())
  }

  suspend fun fireTarget(target: FireTarget) {
    val sourceTank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    val targetTank = battle.players
      .mapNotNull { player -> player.tank }
      .single { tank -> tank.id == target.target }
    if(targetTank.state != TankState.Active) return

    battle.damageProcessor.dealDamage(sourceTank, targetTank, 100.0, false)

    processSplashTargets(target.hitPointWorld.toVector(), target.splashTargetIds, target.splashTargetDistances)

    Command(CommandName.ShotTarget, sourceTank.id, target.toJson()).send(battle.players.exclude(player).ready())
  }

  private suspend fun processSplashTargets(hitPoint: Vector3, ids: List<String>, distances: List<String>) {
    val sourceTank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    ids.forEach { id ->
      val targetTank = battle.players
        .mapNotNull { player -> player.tank }
        .single { tank -> tank.id == id }
      if(targetTank.state != TankState.Active) return

      val distance = hitPoint.distanceTo(targetTank.position) * Vector3Constants.TO_METERS
      val damage = damageCalculator.calculate(sourceTank.weapon, distance, splash = true)
      if(damage.damage < 0) return@forEach

      battle.damageProcessor.dealDamage(sourceTank, targetTank, damage.damage, damage.isCritical)
    }
  }
}
