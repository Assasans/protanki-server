package jp.assasans.protanki.server.quests

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

enum class ServerDailyRewardType(val key: Int) {
  Crystals(1),
  Premium(2),
  ;

  companion object {
    private val map = values().associateBy(ServerDailyRewardType::key)

    fun get(key: Int) = map[key]
  }
}

@Converter
class ServerDailyRewardTypeConverter : AttributeConverter<ServerDailyRewardType, Int> {
  override fun convertToDatabaseColumn(type: ServerDailyRewardType?): Int? = type?.key
  override fun convertToEntityAttribute(key: Int?): ServerDailyRewardType? = key?.let(ServerDailyRewardType::get)
}
