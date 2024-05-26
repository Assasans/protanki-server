package jp.assasans.protanki.server.battles

import java.math.RoundingMode
import kotlin.random.Random
import mu.KotlinLogging
import jp.assasans.protanki.server.battles.weapons.WeaponHandler
import jp.assasans.protanki.server.extensions.nextGaussianRange
import jp.assasans.protanki.server.garage.WeaponDamage
import jp.assasans.protanki.server.math.Vector3Constants

interface IDamageCalculator {
  fun calculate(weapon: WeaponHandler, distance: Double, splash: Boolean = false): DamageCalculateResult

  fun getRandomDamage(range: WeaponDamage.Range): Double
  fun getWeakeningMultiplier(weakening: WeaponDamage.Weakening, distance: Double): Double
  fun getSplashMultiplier(splash: WeaponDamage.Splash, distance: Double): Double
}

fun IDamageCalculator.calculate(source: BattleTank, target: BattleTank): DamageCalculateResult {
  val distance = source.distanceTo(target) * Vector3Constants.TO_METERS
  return calculate(source.weapon, distance)
}

class DamageCalculateResult(
  val damage: Double,
  val weakening: Double,
  val isCritical: Boolean
)

class DamageCalculator : IDamageCalculator {
  private val logger = KotlinLogging.logger { }

  override fun calculate(weapon: WeaponHandler, distance: Double, splash: Boolean): DamageCalculateResult {
    val config = weapon.item.modification.damage

    val baseDamage = config.range?.let { range -> getRandomDamage(range) }
                     ?: config.fixed?.value
                     ?: throw IllegalStateException("No base damage component found for ${weapon.item.mountName}")
    val weakening = config.weakening?.let { getWeakeningMultiplier(it, distance) } ?: 1.0
    val splashDamage = if(splash) config.splash?.let { getSplashMultiplier(it, distance) } ?: 1.0 else 1.0

    var damage = baseDamage
    damage *= weakening
    damage *= splashDamage

    logger.debug {
      buildString {
        append("${weapon.item.marketItem.name} M${weapon.item.modificationIndex} -> (base: ")
        append(baseDamage.toBigDecimal().setScale(3, RoundingMode.HALF_UP).toDouble())
        append(") * (weakening: ")
        append(weakening.toBigDecimal().setScale(3, RoundingMode.HALF_UP).toDouble())
        append(") = ")
        append(damage.toBigDecimal().setScale(3, RoundingMode.HALF_UP).toDouble())
      }
    }

    return DamageCalculateResult(damage, weakening, isCritical = false)
  }

  override fun getRandomDamage(range: WeaponDamage.Range): Double {
    return Random.nextGaussianRange(range.from, range.to)
  }

  override fun getWeakeningMultiplier(weakening: WeaponDamage.Weakening, distance: Double): Double {
    val maximumDamageRadius = weakening.from
    val minimumDamageRadius = weakening.to
    val minimumDamageMultiplier = weakening.minimum

    if(maximumDamageRadius >= minimumDamageRadius) throw IllegalArgumentException("maximumDamageRadius must be more than minimumDamageRadius")

    return when {
      distance <= maximumDamageRadius -> 1.0
      distance >= minimumDamageRadius -> minimumDamageMultiplier
      else                            -> minimumDamageMultiplier + (minimumDamageRadius - distance) * (1.0 - minimumDamageMultiplier) / (minimumDamageRadius - maximumDamageRadius)
    }
  }

  // TODO(Assasans): Incorrect implementation
  override fun getSplashMultiplier(splash: WeaponDamage.Splash, distance: Double): Double {
    val minimumDamage = splash.from
    val maximumDamage = splash.to
    val radius = splash.radius

    if(minimumDamage > maximumDamage) throw IllegalArgumentException("maximumDamage must be more than minimumDamage")

    return when {
      distance >= radius -> minimumDamage
      else               -> (1.0 - distance / radius) * (maximumDamage - minimumDamage)
    }
  }
}
