package jp.assasans.protanki.server.quests

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jp.assasans.protanki.server.client.SocketLocale
import jp.assasans.protanki.server.client.User
import jp.assasans.protanki.server.utils.LocalizedString
import jp.assasans.protanki.server.utils.toLocalizedString

@Entity
@DiscriminatorValue("join_battle_map")
class JoinBattleMapQuest(
  id: Int,
  user: User,
  questIndex: Int,

  current: Int,
  required: Int,

  new: Boolean,
  completed: Boolean,

  rewards: MutableList<ServerDailyQuestReward>,

  val map: String
) : ServerDailyQuest(
  id, user, questIndex,
  current, required,
  new, completed,
  rewards
) {
  override val description: LocalizedString
    get() = mapOf(
      SocketLocale.English to "Join battles on $map map",
      SocketLocale.Russian to "Зайди в битвы на карте $map"
    ).toLocalizedString()
}
