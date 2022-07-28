package jp.assasans.protanki.server.serialization.database

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import jp.assasans.protanki.server.BonusType

@Converter
class BonusTypeConverter : AttributeConverter<BonusType, Int> {
  override fun convertToDatabaseColumn(type: BonusType?): Int? = type?.id
  override fun convertToEntityAttribute(key: Int?): BonusType? = key?.let(BonusType::getById)
}
