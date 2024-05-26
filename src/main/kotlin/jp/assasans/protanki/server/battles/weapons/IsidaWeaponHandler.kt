package jp.assasans.protanki.server.battles.weapons

import jp.assasans.protanki.server.battles.*
import jp.assasans.protanki.server.battles.mode.TeamModeHandler
import jp.assasans.protanki.server.client.send
import jp.assasans.protanki.server.client.weapons.isida.IsidaFireMode
import jp.assasans.protanki.server.client.weapons.isida.ResetTarget
import jp.assasans.protanki.server.client.weapons.isida.SetTarget
import jp.assasans.protanki.server.client.weapons.isida.StartFire
import jp.assasans.protanki.server.client.toJson
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.garage.ServerGarageUserItemWeapon

class IsidaWeaponHandler(
  player: BattlePlayer,
  weapon: ServerGarageUserItemWeapon
) : WeaponHandler(player, weapon) {
  private var fireStarted = false

  suspend fun setTarget(setTarget: SetTarget) {
    val tank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    Command(CommandName.ClientSetTarget, tank.id, setTarget.toJson()).send(battle.players.exclude(player).ready())
  }

  suspend fun resetTarget(resetTarget: ResetTarget) {
    val tank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    Command(CommandName.ClientResetTarget, tank.id, resetTarget.toJson()).send(battle.players.exclude(player).ready())
  }

  suspend fun fireStart(startFire: StartFire) {
    val sourceTank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    val targetTank = battle.players
      .mapNotNull { player -> player.tank }
      .single { tank -> tank.id == startFire.target }
    if(targetTank.state != TankState.Active) return

    val fireMode = when(battle.modeHandler) {
      is TeamModeHandler -> if(targetTank.player.team == sourceTank.player.team) IsidaFireMode.Heal else IsidaFireMode.Damage
      else               -> IsidaFireMode.Damage
    }

    // TODO(Assasans): Damage timing is not checked on server, exploitation is possible
    if(fireStarted) {
      when(fireMode) {
        IsidaFireMode.Damage -> {
          battle.damageProcessor.dealDamage(sourceTank, targetTank, 20.0, isCritical = false)
          battle.damageProcessor.heal(sourceTank, 10.0)
        }

        IsidaFireMode.Heal   -> battle.damageProcessor.heal(sourceTank, targetTank, 20.0)
      }
      return
    }

    fireStarted = true

    val setTarget = SetTarget(
      physTime = startFire.physTime,
      target = startFire.target,
      incarnation = startFire.incarnation,
      localHitPoint = startFire.localHitPoint,
      actionType = fireMode
    )

    Command(CommandName.ClientSetTarget, sourceTank.id, setTarget.toJson()).send(battle.players.exclude(player).ready())
  }

  suspend fun fireStop() {
    val tank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    fireStarted = false

    Command(CommandName.ClientStopFire, tank.id).send(battle.players.exclude(player).ready())
  }
}
