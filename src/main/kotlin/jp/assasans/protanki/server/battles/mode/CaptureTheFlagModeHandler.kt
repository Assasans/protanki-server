package jp.assasans.protanki.server.battles.mode

import jp.assasans.protanki.server.battles.Battle
import jp.assasans.protanki.server.battles.BattleMode
import jp.assasans.protanki.server.battles.BattlePlayer
import jp.assasans.protanki.server.battles.BattleTeam
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.toVector

class CaptureTheFlagModeHandler(battle: Battle) : TeamModeHandler(battle) {
  companion object {
    fun builder(): BattleModeHandlerBuilder = { battle -> CaptureTheFlagModeHandler(battle) }
  }

  override val mode: BattleMode get() = BattleMode.CaptureTheFlag

  private val redFlag = battle.map.flags[BattleTeam.Red] ?: throw throw IllegalStateException("Map does not have a red flag")
  private val blueFlag = battle.map.flags[BattleTeam.Blue] ?: throw throw IllegalStateException("Map does not have a blue flag")

  private val flagOffsetZ = 80

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
