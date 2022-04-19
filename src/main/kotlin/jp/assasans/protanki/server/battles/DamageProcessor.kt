package jp.assasans.protanki.server.battles

import jp.assasans.protanki.server.battles.mode.TeamModeHandler
import jp.assasans.protanki.server.client.send
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName

enum class DamageType(val id: Int, val key: String) {
  Normal(0, "NORMAL"),
  Critical(1, "CRITICAL"),
  Kill(2, "FATAL"),
  Heal(3, "HEAL");

  companion object {
    private val map = values().associateBy(DamageType::key)

    fun get(key: String) = map[key]
  }
}

interface IDamageProcessor {
  val battle: Battle

  suspend fun dealDamage(source: BattleTank, target: BattleTank, damage: Double, isCritical: Boolean)
}

class DamageProcessor(
  override val battle: Battle
) : IDamageProcessor {
  override suspend fun dealDamage(source: BattleTank, target: BattleTank, damage: Double, isCritical: Boolean) {
    if(!battle.properties[BattleProperty.DamageEnabled]) return
    if(battle.modeHandler is TeamModeHandler) {
      if(source.player.team == target.player.team && !battle.properties[BattleProperty.FriendlyFireEnabled]) return
    }

    var damageType = if(isCritical) DamageType.Critical else DamageType.Normal
    val healthDamage = damage * 30 // TODO(Assasans)

    target.health = (target.health - healthDamage).coerceAtLeast(0.0)
    target.updateHealth()
    if(target.health <= 0.0) {
      damageType = DamageType.Kill
      target.killBy(source)
    }

    Command(CommandName.DamageTank, listOf(target.id, damage.toString(), damageType.key)).send(source)
  }
}
