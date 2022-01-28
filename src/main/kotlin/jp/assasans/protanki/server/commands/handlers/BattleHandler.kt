package jp.assasans.protanki.server.commands.handlers

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.battles.TankState
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler

class BattleHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val json by inject<Moshi>()

  @CommandHandler(CommandName.GetInitDataLocalTank)
  suspend fun getInitDataLocalTank(socket: UserSocket) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")

    player.initLocal()
  }

  @CommandHandler(CommandName.Move)
  suspend fun move(socket: UserSocket, data: MoveData) {
    moveInternal(socket, data)
  }

  @CommandHandler(CommandName.FullMove)
  suspend fun fullMove(socket: UserSocket, data: FullMoveData) {
    moveInternal(socket, data)
  }

  private suspend fun moveInternal(socket: UserSocket, data: MoveData) {
    logger.debug { "Tank move: [ ${data.position.x}, ${data.position.y}, ${data.position.z} ]" }

    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")

    if(tank.state != TankState.SemiActive && tank.state !== TankState.Active) {
      logger.warn { "Invalid tank state for movement: ${tank.state}" }

      // Rollback move
      /*
      Command(
        CommandName.ClientFullMove,
        listOf(
          json.adapter(ClientFullMoveData::class.java).toJson(
            ClientFullMoveData(
              tankId = tank.id,
              physTime = data.physTime + 299,
              control = 0,
              specificationID = 0,
              position = tank.position.toVectorData(),
              linearVelocity = Vector3Data(),
              orientation = tank.orientation.toEulerAngles().toVectorData(),
              angularVelocity = Vector3Data(),
              turretDirection = 0.0
            )
          )
        )
      ).send(socket)
      */

      return
    }

    tank.position.copyFrom(data.position.toVector())
  }
}
