package jp.assasans.protanki.server.commands.handlers

import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.battles.TankState
import jp.assasans.protanki.server.battles.sendTo
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler

class BattleHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val json by inject<Moshi>()

  @CommandHandler(CommandName.Ping)
  suspend fun ping(socket: UserSocket) {
    val player = socket.battlePlayer ?: return
    if(!player.stage2Initialized) {
      player.stage2Initialized = true

      logger.info { "Init battle..." }

      player.initStage2()
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
    }

    tank.position.copyFrom(data.position.toVector())
    tank.orientation.fromEulerAngles(data.orientation.toVector())

    if(data is FullMoveData) {
      val count = Command(
        CommandName.ClientFullMove, listOf(
          ClientFullMoveData(tank.id, data).toJson()
        )
      ).sendTo(player.battle, exclude = player)

      logger.debug { "Synced full move to $count players" }
    } else {
      val count = Command(
        CommandName.ClientMove, listOf(
          ClientMoveData(tank.id, data).toJson()
        )
      ).sendTo(player.battle, exclude = player)

      logger.debug { "Synced move to $count players" }
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
      CommandName.ClientRotateTurret, listOf(
        ClientRotateTurretData(tank.id, data).toJson()
      )
    ).sendTo(player.battle, exclude = player)

    logger.debug { "Synced rotate turret to $count players" }
  }

  @CommandHandler(CommandName.MovementControl)
  suspend fun movementControl(socket: UserSocket, data: MovementControlData) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")

    if(tank.state != TankState.SemiActive && tank.state !== TankState.Active) {
      logger.warn { "Invalid tank state for movement control: ${tank.state}" }
    }

    val count = Command(
      CommandName.ClientMovementControl, listOf(
        ClientMovementControlData(tank.id, data).toJson()
      )
    ).sendTo(player.battle, exclude = player)

    logger.debug { "Synced movement control to $count players" }
  }

  @CommandHandler(CommandName.SelfDestruct)
  suspend fun selfDestruct(socket: UserSocket) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")

    logger.debug { "Started self-destruct for ${socket.user!!.username}" }

    tank.selfDestruct()
  }

  @CommandHandler(CommandName.ReadyToRespawn)
  suspend fun readyToRespawn(socket: UserSocket) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")

    player.respawn()
  }

  @CommandHandler(CommandName.ReadyToSpawn)
  suspend fun readyToSpawn(socket: UserSocket) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val tank = player.tank ?: throw Exception("No Tank")

    tank.spawn()
    player.spawnTankForAnother()

    delay(1500)
    tank.activate()
  }

  @CommandHandler(CommandName.ExitFromBattle)
  suspend fun exitFromBattle(socket: UserSocket, destinationScreen: String) {
    val player = socket.battlePlayer ?: throw Exception("No BattlePlayer")
    val battle = player.battle
    battle.players.remove(player)

    Command(CommandName.BattlePlayerLeaveDm, listOf(player.user.username)).sendTo(battle, exclude = player)
    Command(CommandName.BattlePlayerRemove, listOf(player.user.username)).sendTo(battle, exclude = player)

    Command(CommandName.UnloadBattle).send(socket)

    socket.initChatMessages()

    when(destinationScreen) {
      "BATTLE_SELECT" -> {
        Command(CommandName.StartLayoutSwitch, listOf("BATTLE_SELECT")).send(socket)
        socket.loadLobbyResources()
        Command(CommandName.EndLayoutSwitch, listOf("BATTLE_SELECT", "BATTLE_SELECT")).send(socket)

        socket.screen = Screen.BattleSelect
        socket.initBattleList()

        logger.debug { "Select battle ${battle.id} -> ${battle.title}" }

        battle.selectFor(socket)
        battle.showInfoFor(socket)
      }

      "GARAGE"        -> {
        Command(CommandName.StartLayoutSwitch, listOf("GARAGE")).send(socket)
        socket.screen = Screen.Garage
        socket.loadGarageResources()
        socket.initGarage()
        Command(CommandName.EndLayoutSwitch, listOf("GARAGE", "GARAGE")).send(socket)
      }
    }
  }
}
