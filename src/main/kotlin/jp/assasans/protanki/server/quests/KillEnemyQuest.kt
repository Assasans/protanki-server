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
@DiscriminatorValue("kill_enemy")
class KillEnemyQuest(
  id: Int,
  user: User,
  questIndex: Int,

  current: Int,
  required: Int,

  new: Boolean,
  completed: Boolean,

  rewards: MutableList<ServerDailyQuestReward>,

  @Convert(converter = BattleModeConverter::class)
  val mode: BattleMode?
) : ServerDailyQuest(
  id, user, questIndex,
  current, required,
  new, completed,
  rewards
) {
  // TODO(Assasans): Localize mode name
  override val description: LocalizedString
    get() = mapOf(
      SocketLocale.English to if(mode != null) "Kill enemy tanks in ${mode!!.name} mode" else "Kill enemy tanks",
      SocketLocale.Russian to if(mode != null) "Уничтожь противников в режиме ${mode!!.name}" else "Уничтожь противников"
    ).toLocalizedString()
}
