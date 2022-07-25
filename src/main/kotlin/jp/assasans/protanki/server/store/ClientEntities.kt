package jp.assasans.protanki.server.store

import com.squareup.moshi.Json
import jp.assasans.protanki.server.utils.ClientLocalizedString

data class OpenStoreWrapperData(
  @Json val have_double_crystals: Boolean = false,
  @Json val data: String
)

data class OpenStoreData(
  @Json val categories: List<StoreCategory>,
  @Json val items: List<StoreItem>
)

data class StoreCategory(
  @Json val category_id: String,
  @Json val header_text: ClientLocalizedString,
  @Json val description: ClientLocalizedString
)

data class StoreItem(
  @Json val category_id: String,
  @Json val item_id: String,
  @Json(name = "additional_data") val properties: StoreItemProperties
)

data class StoreItemProperties(
  @Json val price: Double,
  @Json val currency: String,

  /* CrystalPackageItem */
  @Json(name = "crystalls_count") val crystals: Int?,
  @Json(name = "bonus_crystalls") val bonusCrystals: Int?,

  /* PremiumPackageItem */
  /**
   * Premium subscription duration in days
   */
  @Json(name = "premium_duration") val premiumDuration: Int?
)
