package jp.assasans.protanki.server.battles.effect

import kotlin.time.Duration.Companion.seconds
import jp.assasans.protanki.server.battles.BattleTank
import jp.assasans.protanki.server.battles.sendTo
import jp.assasans.protanki.server.client.ChangeTankSpecificationData
import jp.assasans.protanki.server.client.toJson
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName

class NitroEffect(
  tank: BattleTank,
  val multiplier: Double = 1.3
) : TankEffect(
  tank,
  duration = 55.seconds,
  cooldown = 20.seconds
) {
  override val info: EffectInfo
    get() = EffectInfo(
      id = 4,
      name = "n2o"
    )

  var specification = ChangeTankSpecificationData.fromPhysics(tank.hull.modification.physics, tank.weapon.item.modification.physics)
    private set

  override suspend fun activate() {
    specification.speed *= multiplier
    sendSpecificationChange()
  }

  override suspend fun deactivate() {
    specification.speed /= multiplier
    sendSpecificationChange()
  }

  private suspend fun sendSpecificationChange() {
    Command(CommandName.ChangeTankSpecification, tank.id, specification.toJson()).sendTo(tank.battle)
  }
}
