package jp.assasans.protanki.server.quests

import jakarta.persistence.*
import java.io.Serializable

@Embeddable
class ServerDailyQuestRewardId(
  @ManyToOne
  val quest: ServerDailyQuest,
  @Column(name = "rewardIndex") val index: Int // INDEX is a reserved word in MariaDB
) : Serializable {
  override fun equals(other: Any?): Boolean {
    if(this === other) return true
    if(other !is ServerDailyQuestRewardId) return false

    if(quest != other.quest) return false
    if(index != other.index) return false

    return true
  }

  override fun hashCode(): Int {
    var result = quest.hashCode()
    result = 31 * result + index.hashCode()
    return result
  }
}

@Entity
@Table(name = "daily_quest_rewards")
class ServerDailyQuestReward(
  quest: ServerDailyQuest,
  index: Int,

  @Convert(converter = ServerDailyRewardTypeConverter::class)
  val type: ServerDailyRewardType,
  val count: Int
) {
  @EmbeddedId
  val id: ServerDailyQuestRewardId = ServerDailyQuestRewardId(quest = quest, index = index)

  val quest
    get() = id.quest

  val index
    get() = id.index
}
