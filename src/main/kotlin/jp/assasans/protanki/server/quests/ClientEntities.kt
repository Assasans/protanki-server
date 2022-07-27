package jp.assasans.protanki.server.quests

import com.squareup.moshi.Json

data class OpenQuestsData(
  @Json val weeklyQuestDescription: WeeklyQuestDescriptionData,
  @Json val quests: List<DailyQuest>
)

data class WeeklyQuestDescriptionData(
  @Json val currentQuestLevel: Int = 0,
  @Json val currentQuestStreak: Int = 0,
  @Json val doneForToday: Boolean = false,
  @Json val questImage: Int = 123341,
  @Json val rewardImage: Int = 123345
)

data class DailyQuest(
  @Json val canSkipForFree: Boolean = true,
  @Json val description: String,
  @Json val finishCriteria: Int,
  @Json val image: Int = 412322,
  @Json val questId: Int = 58366,
  @Json val progress: Int,
  @Json val skipCost: Int = 100,
  @Json val prizes: List<DailyQuestPrize>
)

data class DailyQuestPrize(
  @Json val name: String,
  @Json val count: Int
)

data class SkipDailyQuestResponseData(
  @Json val questId: Int,
  @Json val quest: DailyQuest
)
