package jp.assasans.protanki.server.battles

import mu.KotlinLogging
import jp.assasans.protanki.server.client.UserSocket
import jp.assasans.protanki.server.math.Quaternion
import jp.assasans.protanki.server.math.Vector3

interface ITickHandler {
  suspend fun tick() {}
}

class BattleTank(
  val id: String,
  val player: BattlePlayer,
  val incarnation: Int = 1,
  var position: Vector3,
  var orientation: Quaternion
) : ITickHandler {
  private val logger = KotlinLogging.logger { }

  val battle: Battle
    get() = player.battle
}

class BattlePlayer(
  val socket: UserSocket,
  val battle: Battle,
  var tank: BattleTank?
) : ITickHandler {
  private val logger = KotlinLogging.logger { }

  var incarnation: Int = 0

  fun spawn(): BattleTank {
    incarnation++

    val tank = BattleTank(
      id = socket.user!!.username,
      player = this,
      incarnation = incarnation,
      position = Vector3(0.0, 0.0, 1000.0),
      orientation = Quaternion()
    )

    this.tank = tank
    return tank
  }
}

class Battle(
  val id: String
) : ITickHandler {
  private val logger = KotlinLogging.logger { }

  companion object {
    private var lastId: Int = 1

    fun generateId(): String {
      return "test-${lastId++}"
    }
  }

  override suspend fun tick() {
    players.forEach { player ->
      logger.trace { "Running tick handler for player ${player.socket.user!!.username}" }
      player.tick()
    }
  }

  val players: MutableList<BattlePlayer> = mutableListOf()
}

interface IBattleProcessor : ITickHandler {
  val battles: MutableList<Battle>
}

class BattleProcessor : IBattleProcessor {
  private val logger = KotlinLogging.logger { }

  override val battles: MutableList<Battle> = mutableListOf()

  override suspend fun tick() {
    battles.forEach { battle ->
      logger.trace { "Running tick handler for battle ${battle.id}" }
      battle.tick()
    }
  }
}
