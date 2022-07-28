package jp.assasans.protanki.server.quests

import jakarta.persistence.Convert
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jp.assasans.protanki.server.BonusType
import jp.assasans.protanki.server.client.SocketLocale
import jp.assasans.protanki.server.client.User
import jp.assasans.protanki.server.serialization.database.BonusTypeConverter
import jp.assasans.protanki.server.utils.LocalizedString
import jp.assasans.protanki.server.utils.toLocalizedString

@Entity
@DiscriminatorValue("take_bonus")
class TakeBonusQuest(
  id: Int,
  user: User,
  questIndex: Int,

  current: Int,
  required: Int,

  new: Boolean,
  completed: Boolean,

  rewards: MutableList<ServerDailyQuestReward>,

  @Convert(converter = BonusTypeConverter::class)
  val bonus: BonusType
) : ServerDailyQuest(
  id, user, questIndex,
  current, required,
  new, completed,
  rewards
) {
  // TODO(Assasans): Localize bonus name
  override val description: LocalizedString
    get() = mapOf(
      SocketLocale.English to "Take ${bonus.name}",
      SocketLocale.Russian to "Подбери ${bonus.name}"
    ).toLocalizedString()
}
