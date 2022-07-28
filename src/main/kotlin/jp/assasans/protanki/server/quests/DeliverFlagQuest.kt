package jp.assasans.protanki.server.quests

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jp.assasans.protanki.server.client.SocketLocale
import jp.assasans.protanki.server.client.User
import jp.assasans.protanki.server.utils.LocalizedString
import jp.assasans.protanki.server.utils.toLocalizedString

@Entity
@DiscriminatorValue("deliver_flag")
class DeliverFlagQuest(
  id: Int,
  user: User,
  questIndex: Int,

  current: Int,
  required: Int,

  new: Boolean,
  completed: Boolean,

  rewards: MutableList<ServerDailyQuestReward>
) : ServerDailyQuest(
  id, user, questIndex,
  current, required,
  new, completed,
  rewards
) {
  override val description: LocalizedString
    get() = mapOf(
      SocketLocale.English to "Deliver the flags",
      SocketLocale.Russian to "Доставь флаги"
    ).toLocalizedString()
}
