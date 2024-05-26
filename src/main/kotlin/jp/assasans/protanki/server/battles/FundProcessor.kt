package jp.assasans.protanki.server.battles

import org.koin.core.component.KoinComponent
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName

interface IFundProcessor {
  val battle: Battle
  var fund: Int

  suspend fun updateFund()
}

class FundProcessor(
  override val battle: Battle
) : IFundProcessor, KoinComponent {
  override var fund: Int = 0

  override suspend fun updateFund() {
    Command(CommandName.ChangeFund, fund.toString()).sendTo(battle)
  }
}
