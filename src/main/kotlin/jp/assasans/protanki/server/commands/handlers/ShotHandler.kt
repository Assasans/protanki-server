package jp.assasans.protanki.server.commands.handlers

import com.squareup.moshi.Moshi
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.battles.RailgunWeaponHandler
import jp.assasans.protanki.server.battles.ThunderWeaponHandler
import jp.assasans.protanki.server.client.UserSocket
import jp.assasans.protanki.server.client.railgun.FireTarget
import jp.assasans.protanki.server.commands.*

class ShotHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val json by inject<Moshi>()

  @CommandHandler(CommandName.StartFire)
  @ArgsBehaviour(ArgsBehaviourType.Raw)
  suspend fun startFire(socket: UserSocket, args: CommandArgs) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")

    logger.info { "StartFire: ${args.get(0)}" }

    when(tank.weapon) {
      is RailgunWeaponHandler -> tank.weapon.fireStart()
    }
  }

  @CommandHandler(CommandName.Fire)
  @ArgsBehaviour(ArgsBehaviourType.Raw)
  suspend fun fire(socket: UserSocket, args: CommandArgs) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")

    logger.info { "Fire: ${args.get(0)}" }

    when(tank.weapon) {
      is ThunderWeaponHandler -> tank.weapon.fire(args.getAs(0))
    }
  }

  @CommandHandler(CommandName.FireStatic)
  @ArgsBehaviour(ArgsBehaviourType.Raw)
  suspend fun fireStatic(socket: UserSocket, args: CommandArgs) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")

    logger.info { "FireStatic: ${args.get(0)}" }

    when(tank.weapon) {
      is ThunderWeaponHandler -> tank.weapon.fireStatic(args.getAs(0))
    }
  }

  @CommandHandler(CommandName.FireTarget)
  @ArgsBehaviour(ArgsBehaviourType.Raw)
  suspend fun fireTarget(socket: UserSocket, args: CommandArgs) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")

    logger.info { "FireTarget: ${args.get(0)}" }

    when(tank.weapon) {
      is RailgunWeaponHandler -> tank.weapon.fireTarget(args.getAs(0))
      is ThunderWeaponHandler -> tank.weapon.fireTarget(args.getAs(0))
    }
  }
}
