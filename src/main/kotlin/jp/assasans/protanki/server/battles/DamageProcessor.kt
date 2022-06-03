package jp.assasans.protanki.server.battles

import jp.assasans.protanki.server.battles.effect.DoubleArmorEffect
import jp.assasans.protanki.server.battles.effect.DoubleDamageEffect
import jp.assasans.protanki.server.battles.effect.TankEffect
import jp.assasans.protanki.server.battles.mode.TeamModeHandler
import jp.assasans.protanki.server.client.send
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.extensions.singleOrNullOf

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

  suspend fun dealDamage(source: BattleTank, target: BattleTank, damage: Double, isCritical: Boolean, ignoreSourceEffects: Boolean = false)
  suspend fun dealDamage(target: BattleTank, damage: Double, isCritical: Boolean): DamageType

  suspend fun heal(source: BattleTank, target: BattleTank, heal: Double)
  suspend fun heal(target: BattleTank, heal: Double)
}

class DamageProcessor(
  override val battle: Battle
) : IDamageProcessor {
  override suspend fun dealDamage(
    source: BattleTank,
    target: BattleTank,
    damage: Double,
    isCritical: Boolean,
    ignoreSourceEffects: Boolean
  ) {
    var totalDamage = damage

    if(!battle.properties[BattleProperty.DamageEnabled]) return
    if(battle.modeHandler is TeamModeHandler) {
      if(source.player.team == target.player.team && !battle.properties[BattleProperty.FriendlyFireEnabled]) return
    }

    if(!ignoreSourceEffects) {
      source.effects.singleOrNullOf<TankEffect, DoubleDamageEffect>()?.let { effect ->
        totalDamage *= effect.multiplier
      }
    }

    target.effects.singleOrNullOf<TankEffect, DoubleArmorEffect>()?.let { effect ->
      totalDamage /= effect.multiplier
    }

    val damageType = dealDamage(target, totalDamage, isCritical)
    if(damageType == DamageType.Kill) {
      target.killBy(source)
    }

    Command(CommandName.DamageTank, listOf(target.id, totalDamage.toString(), damageType.key)).send(source)
  }

  override suspend fun dealDamage(target: BattleTank, damage: Double, isCritical: Boolean): DamageType {
    var damageType = if(isCritical) DamageType.Critical else DamageType.Normal
    val healthDamage = damage * 30 // TODO(Assasans)

    target.health = (target.health - healthDamage).coerceAtLeast(0.0)
    target.updateHealth()
    if(target.health <= 0.0) {
      damageType = DamageType.Kill
    }

    return damageType
  }

  override suspend fun heal(source: BattleTank, target: BattleTank, heal: Double) {
    heal(target, heal)

    Command(CommandName.DamageTank, listOf(target.id, heal.toString(), DamageType.Heal.key)).send(source)
  }

  override suspend fun heal(target: BattleTank, heal: Double) {
    val healthChange = heal * 30 // TODO(Assasans)

    target.health = (target.health + healthChange).coerceAtMost(TankConstants.MAX_HEALTH)
    target.updateHealth()
  }
}
