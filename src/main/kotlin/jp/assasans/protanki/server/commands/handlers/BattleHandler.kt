package jp.assasans.protanki.server.commands.handlers

import kotlin.time.Duration.Companion.seconds
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.battles.*
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler
import jp.assasans.protanki.server.extensions.launchDelayed

class BattleHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val json by inject<Moshi>()

  @CommandHandler(CommandName.Ping)
  suspend fun ping(socket: UserSocket) {
    val player = socket.battlePlayer ?: return

    player.sequence++

    val initBattle = if(player.isSpectator) player.sequence == BattlePlayerConstants.SPECTATOR_INIT_SEQUENCE
    else player.sequence == BattlePlayerConstants.USER_INIT_SEQUENCE
    if(initBattle) {
      logger.debug { "Init battle for ${player.user.username}..." }
      player.initBattle()
    }

    Command(CommandName.Pong).send(socket)
  }

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
    // logger.trace { "Tank move: [ ${data.position.x}, ${data.position.y}, ${data.position.z} ]" }

    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")

    if(tank.state != TankState.SemiActive && tank.state !== TankState.Active) {
      logger.warn { "Invalid tank state for movement: ${tank.state}" }

      // Rollback move
      /*
      Command(
        CommandName.ClientFullMove,
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
      ).send(socket)
      */
    }

    tank.position.copyFrom(data.position.toVector())
    tank.orientation.fromEulerAngles(data.orientation.toVector())

    if(data is FullMoveData) {
      val count = Command(
        CommandName.ClientFullMove,
        ClientFullMoveData(tank.id, data).toJson()
      ).sendTo(player.battle, exclude = player)

      logger.trace { "Synced full move to $count players" }
    } else {
      val count = Command(
        CommandName.ClientMove,
        ClientMoveData(tank.id, data).toJson()
      ).sendTo(player.battle, exclude = player)

      logger.trace { "Synced move to $count players" }
    }
  }

  @CommandHandler(CommandName.RotateTurret)
  suspend fun rotateTurret(socket: UserSocket, data: RotateTurretData) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")

    if(tank.state != TankState.SemiActive && tank.state !== TankState.Active) {
      logger.warn { "Invalid tank state for rotate turret: ${tank.state}" }
    }

    val count = Command(
      CommandName.ClientRotateTurret,
      ClientRotateTurretData(tank.id, data).toJson()
    ).sendTo(player.battle, exclude = player)

    logger.trace { "Synced rotate turret to $count players" }
  }

  @CommandHandler(CommandName.MovementControl)
  suspend fun movementControl(socket: UserSocket, data: MovementControlData) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")

    if(tank.state != TankState.SemiActive && tank.state !== TankState.Active) {
      logger.warn { "Invalid tank state for movement control: ${tank.state}" }
    }

    val count = Command(
      CommandName.ClientMovementControl,
      ClientMovementControlData(tank.id, data).toJson()
    ).sendTo(player.battle, exclude = player)

    logger.trace { "Synced movement control to $count players" }
  }

  @CommandHandler(CommandName.SelfDestruct)
  suspend fun selfDestruct(socket: UserSocket) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")

    logger.debug { "Started self-destruct for ${socket.user!!.username}" }

    if(player.battle.properties[BattleProperty.InstantSelfDestruct]) {
      tank.selfDestruct()
    } else {
      tank.selfDestructing = true
      tank.coroutineScope.launchDelayed(10.seconds) {
        tank.selfDestructing = false
        tank.selfDestruct()
      }
    }
  }

  @CommandHandler(CommandName.ReadyToRespawn)
  suspend fun readyToRespawn(socket: UserSocket) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")

    player.respawn()
  }

  @CommandHandler(CommandName.ReadyToSpawn)
  suspend fun readyToSpawn(socket: UserSocket) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")

    val newTank = player.createTank()
    newTank.position = tank.position
    newTank.orientation = tank.orientation

    newTank.spawn()

    delay(1500)
    newTank.activate()
  }

  @CommandHandler(CommandName.ExitFromBattle)
  suspend fun exitFromBattle(socket: UserSocket, destinationScreen: String) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val battle = player.battle

    player.deactivate(terminate = true)
    battle.players.remove(player)

    Command(CommandName.UnloadBattle).send(socket)

    socket.initChatMessages()

    when(destinationScreen) {
      "BATTLE_SELECT" -> {
        Command(CommandName.StartLayoutSwitch, "BATTLE_SELECT").send(socket)
        socket.loadLobbyResources()
        Command(CommandName.EndLayoutSwitch, "BATTLE_SELECT", "BATTLE_SELECT").send(socket)

        socket.screen = Screen.BattleSelect
        socket.initBattleList()

        logger.debug { "Select battle ${battle.id} -> ${battle.title}" }

        battle.selectFor(socket)
        battle.showInfoFor(socket)
      }

      "GARAGE"        -> {
        Command(CommandName.StartLayoutSwitch, "GARAGE").send(socket)
        socket.screen = Screen.Garage
        socket.loadGarageResources()
        socket.initGarage()
        Command(CommandName.EndLayoutSwitch, "GARAGE", "GARAGE").send(socket)
      }
    }
  }

  @CommandHandler(CommandName.TriggerMine)
  suspend fun triggerMine(socket: UserSocket, key: String) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")
    val battle = player.battle

    val username = key.substringBeforeLast("_")
    val id = key.substringAfterLast("_").toInt()

    val mine = battle.mineProcessor.mines[id]
    if(mine == null) {
      logger.warn { "Attempt to activate missing mine: $username@$id" }
      return
    }

    mine.trigger(tank)
  }
}
