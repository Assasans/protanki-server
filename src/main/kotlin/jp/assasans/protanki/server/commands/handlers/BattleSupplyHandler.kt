package jp.assasans.protanki.server.commands.handlers

import kotlin.time.Duration.Companion.milliseconds
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import jp.assasans.protanki.server.battles.BattleProperty
import jp.assasans.protanki.server.battles.effect.*
import jp.assasans.protanki.server.client.UserSocket
import jp.assasans.protanki.server.client.send
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler

class BattleSupplyHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  @CommandHandler(CommandName.ActivateItem)
  suspend fun activateItem(socket: UserSocket, item: String) {
    val user = socket.user ?: throw Exception("No User")
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")

    val effect = when(item) {
      "health"        -> RepairKitEffect(tank)
      "armor"         -> DoubleArmorEffect(tank)
      "double_damage" -> DoubleDamageEffect(tank)
      "n2o"           -> NitroEffect(tank)
      "mine"          -> MineEffect(tank)
      else            -> throw Exception("Unknown item: $item")
    }
    effect ?: return

    // Remark: original server sends commands in the following order:
    // 1. TankEffect actions (e.g. ChangeTankSpecification for NitroEffect)
    // 2. ClientActivateItem
    // 3. EnableEffect
    tank.effects.add(effect)
    effect.run()

    var slotBlockTime = 0.milliseconds
    if(effect.duration != null) slotBlockTime += effect.duration
    if(player.battle.properties[BattleProperty.SuppliesCooldownEnabled] && effect.cooldown != null) slotBlockTime += effect.cooldown

    Command(
      CommandName.ClientActivateItem,
      effect.info.name,
      slotBlockTime.inWholeMilliseconds.toString(),
      true.toString() // Decrement item count in HUD (visual)
    ).send(socket)
  }

  @CommandHandler(CommandName.TryActivateBonus)
  suspend fun tryActivateBonus(socket: UserSocket, key: String) {
    val user = socket.user ?: throw Exception("No User")
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    val type = key.substringBeforeLast("_")
    val id = key.substringAfterLast("_").toInt()

    val bonus = battle.bonusProcessor.bonuses[id]
    if(bonus == null) {
      logger.warn { "Attempt to activate missing bonus: $type@$id" }
      return
    }

    if(bonus.type.bonusKey != type) {
      logger.warn { "Attempt to activate bonus ($id) with wrong type. Actual: ${bonus.type}, received $type" }
    }

    battle.bonusProcessor.activate(bonus, tank)
  }
}
