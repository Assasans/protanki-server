package jp.assasans.protanki.server.garage

import com.squareup.moshi.Json
import jp.assasans.protanki.server.serialization.SerializeNull

data class InitGarageItemsData(
  @Json val items: List<GarageItem>,
  @Json val garageBoxID: Int = 170001
)

data class InitGarageMarketData(
  @Json val items: List<GarageItem>,
  @Json val delayMountArmorInSec: Int = 0,
  @Json val delayMountWeaponInSec: Int = 0,
  @Json val delayMountColorInSec: Int = 0
)

open class GarageItem(
  @Json val id: String,
  @Json val index: Int,
  @Json val type: GarageItemType,
  @Json val category: String,
  @Json val isInventory: Boolean,

  @Json val name: String,
  @Json val description: String,

  @Json val rank: Int,
  @Json val next_rank: Int,

  @Json val price: Int,
  @Json val next_price: Int,
  @Json val discount: Discount,

  @Json val baseItemId: Int,
  @Json val previewResourceId: Int,

  /**
   * Remaining time in seconds for time-limited items (premium account, PRO battle, etc.)
   */
  @Json(name = "remainingTimeInSec") val timeLeft: Long,

  @Json(name = "properts") val properties: List<GarageItemProperty>,

  // Weapon / hull
  @Json val modificationID: Int?,
  @Json val object3ds: Int?,

  // Paint
  @Json val coloring: Int?,

  // Supply
  @Json val count: Int?,

  // Kit
  @Json val kit: GarageItemKit?
)

data class GarageItemKit(
  @Json val image: Int,
  @Json val discountInPercent: Int,
  @Json val kitItems: List<GarageItemKitItem>,
  @Json val isTimeless: Boolean,
  @Json val timeLeftInSeconds: Int
)

data class GarageItemKitItem(
  @Json val id: String,
  @Json val count: Int
)

data class Discount(
  @Json val percent: Int,
  @Json val timeLeftInSeconds: Int,
  @Json val timeToStartInSeconds: Int
)

data class GarageItemProperty(
  @Json val property: String,
  @Json @SerializeNull val value: String?,
  @Json @SerializeNull val subproperties: List<GarageItemProperty>?
)
