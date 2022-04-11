package jp.assasans.protanki.server.battles.mode

import jp.assasans.protanki.server.battles.*
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.math.Vector3
import jp.assasans.protanki.server.toVector

abstract class FlagState(val team: BattleTeam)
class FlagOnPedestalState(team: BattleTeam) : FlagState(team)
class FlagDroppedState(team: BattleTeam, val position: Vector3) : FlagState(team)
class FlagCarryingState(team: BattleTeam, val carrier: BattleTank) : FlagState(team)

fun FlagState.asOnPedestal(): FlagOnPedestalState = FlagOnPedestalState(team)
fun FlagState.asDropped(position: Vector3): FlagDroppedState = FlagDroppedState(team, position)
fun FlagState.asCarrying(carrier: BattleTank): FlagCarryingState = FlagCarryingState(team, carrier)

class CaptureTheFlagModeHandler(battle: Battle) : TeamModeHandler(battle) {
  companion object {
    fun builder(): BattleModeHandlerBuilder = { battle -> CaptureTheFlagModeHandler(battle) }
  }

  override val mode: BattleMode get() = BattleMode.CaptureTheFlag

  val flags = mutableMapOf<BattleTeam, FlagState>(
    BattleTeam.Red to FlagOnPedestalState(BattleTeam.Red),
    BattleTeam.Blue to FlagOnPedestalState(BattleTeam.Blue)
  )

  private val flagOffsetZ = 80

  suspend fun captureFlag(flagTeam: BattleTeam, carrier: BattleTank) {
    flags[flagTeam] = flags[flagTeam]!!.asCarrying(carrier) // TODO(Assasans): Non-null assertion

    Command(CommandName.FlagCaptured, listOf(carrier.id, flagTeam.key)).sendTo(battle)
  }

  suspend fun dropFlag(flagTeam: BattleTeam, carrier: BattleTank, position: Vector3) {
    flags[flagTeam] = flags[flagTeam]!!.asDropped(position) // TODO(Assasans): Non-null assertion

    Command(
      CommandName.FlagDropped,
      listOf(
        FlagDroppedData(
          x = position.x,
          y = position.y,
          z = position.z,
          flagTeam = flagTeam
        ).toJson()
      )
    ).sendTo(battle)
  }

  suspend fun deliverFlag(enemyFlagTeam: BattleTeam, flagTeam: BattleTeam, carrier: BattleTank) {
    flags[enemyFlagTeam] = flags[enemyFlagTeam]!!.asOnPedestal() // TODO(Assasans): Non-null assertion
    teamScores.merge(flagTeam, 1, Int::plus)

    Command(CommandName.FlagDelivered, listOf(flagTeam.key, carrier.id)).sendTo(battle)
    updateScores()
  }

  suspend fun returnFlag(flagTeam: BattleTeam, carrier: BattleTank?) {
    flags[flagTeam] = flags[flagTeam]!!.asOnPedestal() // TODO(Assasans): Non-null assertion

    Command(
      CommandName.FlagReturned,
      listOf(
        flagTeam.key,
        carrier?.player?.user?.username ?: null.toString()
      )
    ).sendTo(battle)
  }

  override suspend fun initModeModel(player: BattlePlayer) {
    Command(
      CommandName.InitCtfModel,
      listOf(getCtfModel().toJson())
    ).send(player)
  }

  override suspend fun initPostGui(player: BattlePlayer) {
    Command(
      CommandName.InitFlags,
      listOf(getCtfModel().toJson())
    ).send(player)
  }

  private fun getCtfModel(): InitCtfModelData {
    val flags = battle.map.flags ?: throw IllegalStateException("Map has no flags")
    val redFlag = flags[BattleTeam.Red] ?: throw throw IllegalStateException("Map does not have a red flag")
    val blueFlag = flags[BattleTeam.Blue] ?: throw throw IllegalStateException("Map does not have a blue flag")

    val redFlagPosition = redFlag.position.toVector()
    val blueFlagPosition = blueFlag.position.toVector()

    redFlagPosition.z += flagOffsetZ
    blueFlagPosition.z += flagOffsetZ

    return InitCtfModelData(
      resources = CtfModelResources().toJson(),
      lighting = CtfModelLighting().toJson(),
      basePosRedFlag = redFlagPosition.toVectorData(),
      basePosBlueFlag = blueFlagPosition.toVectorData(),
      posRedFlag = null,
      posBlueFlag = null,
      redFlagCarrierId = null,
      blueFlagCarrierId = null
    )
  }
}
