package jp.assasans.protanki.server.battles

import kotlin.reflect.KClass

class BattleProperty<T : Any> private constructor(val key: String, val type: KClass<T>, val defaultValue: T? = null) {
  companion object {
    private val properties = mutableMapOf<String, BattleProperty<*>>()

    val DamageEnabled = BattleProperty("damage_enabled", Boolean::class, true)
    val FriendlyFireEnabled = BattleProperty("friendly_fire", Boolean::class, false)
    val SelfDamageEnabled = BattleProperty("self_damage_enabled", Boolean::class, true)

    val InstantSelfDestruct = BattleProperty("instant_self_destruct", Boolean::class, false)

    val SuppliesCooldownEnabled = BattleProperty("supplies_cooldown_enabled", Boolean::class, true)

    val DeactivateMinesOnDeath = BattleProperty("deactivate_mines_on_death", Boolean::class, true)

    val ParkourMode = BattleProperty("parkour_mode", Boolean::class, false)

    val RearmingEnabled = BattleProperty("rearming_enabled", Boolean::class, true)

    // TODO(Assasans): Use UserRank?
    val MinRank = BattleProperty("min_rank", Int::class, 1)
    val MaxRank = BattleProperty("min_rank", Int::class, 30)

    // TODO(Assasans): BattleProperty does not allow null values
    val TimeLimit = BattleProperty("time_limit", Int::class, 0)

    fun values() = properties.values.toList()

    fun get(key: String) = getOrNull(key) ?: throw IllegalArgumentException("No such property: $key")
    fun getOrNull(key: String) = properties[key]
  }

  init {
    properties[key] = this
  }
}

class BattleProperties(val properties: MutableMap<BattleProperty<*>, Any> = mutableMapOf()) {
  operator fun <T : Any> get(property: BattleProperty<T>): T = getOrNull(property) ?: property.defaultValue as T

  fun <T : Any> getOrNull(property: BattleProperty<T>): T? {
    val value = properties.getOrDefault(property, null) ?: return null
    return value as T
  }

  operator fun <T : Any> set(property: BattleProperty<T>, value: T) {
    properties[property] = value
  }

  fun <T : Any> setValue(property: BattleProperty<T>, value: Any) {
    if(value::class != property.type) throw IllegalArgumentException("Value is not ${property.type}")
    properties[property] = value
  }
}
