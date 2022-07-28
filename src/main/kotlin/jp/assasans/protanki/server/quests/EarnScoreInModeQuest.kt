package jp.assasans.protanki.server.quests

import jakarta.persistence.Convert
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jp.assasans.protanki.server.battles.BattleMode
import jp.assasans.protanki.server.client.SocketLocale
import jp.assasans.protanki.server.client.User
import jp.assasans.protanki.server.serialization.database.BattleModeConverter
import jp.assasans.protanki.server.utils.LocalizedString
import jp.assasans.protanki.server.utils.toLocalizedString

@Entity
@DiscriminatorValue("earn_score_mode")
class EarnScoreInModeQuest(
  id: Int,
  user: User,
  questIndex: Int,

  current: Int,
  required: Int,

  new: Boolean,
  completed: Boolean,

  rewards: MutableList<ServerDailyQuestReward>,

  @Convert(converter = BattleModeConverter::class)
  val mode: BattleMode
) : ServerDailyQuest(
  id, user, questIndex,
  current, required,
  new, completed,
  rewards
) {
  override val description: LocalizedString
    get() = mapOf(
      SocketLocale.English to "Earn experience in $mode",
      SocketLocale.Russian to "Набери опыт в режиме $mode"
    ).toLocalizedString()
}
