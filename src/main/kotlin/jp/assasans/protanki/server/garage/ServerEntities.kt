package jp.assasans.protanki.server.garage

import kotlin.time.Duration
import com.squareup.moshi.Json
import kotlinx.datetime.Instant

open class ServerGarageItem(
  @Json val id: String,
  @Json val index: Int,
  @Json val type: GarageItemType,

  @Json val name: String,
  @Json val description: String,

  @Json val baseItemId: Int
)

interface IServerGarageItemWithModifications {
  val modifications: Map<Int, ServerGarageItemModification>
}

class ServerGarageItemWeapon(
  id: String,
  index: Int,

  name: String,
  description: String,

  baseItemId: Int,

  @Json override val modifications: Map<Int, ServerGarageItemModification>
) : ServerGarageItem(
  id, index, GarageItemType.Weapon,
  name, description,
  baseItemId
), IServerGarageItemWithModifications

class ServerGarageItemHull(
  id: String,
  index: Int,

  name: String,
  description: String,

  baseItemId: Int,

  @Json override val modifications: Map<Int, ServerGarageItemModification>
) : ServerGarageItem(
  id, index, GarageItemType.Hull,
  name, description,
  baseItemId
), IServerGarageItemWithModifications

class ServerGarageItemPaint(
  id: String,
  index: Int,

  name: String,
  description: String,

  baseItemId: Int,
  @Json val previewResourceId: Int,

  @Json val rank: Int,
  @Json val price: Int,

  @Json val coloring: Int,

  @Json val properties: List<ServerGarageItemProperty>
) : ServerGarageItem(
  id, index, GarageItemType.Paint,
  name, description,
  baseItemId
)

class ServerGarageItemSupply(
  id: String,
  index: Int,

  name: String,
  description: String,

  baseItemId: Int,
  @Json val previewResourceId: Int,

  @Json val rank: Int,
  @Json val price: Int,

  @Json val properties: List<ServerGarageItemProperty>
) : ServerGarageItem(
  id, index, GarageItemType.Supply,
  name, description,
  baseItemId
)

class ServerGarageItemKit(
  id: String,
  index: Int,

  name: String,
  description: String,

  baseItemId: Int,
  @Json val previewResourceId: Int,

  @Json val rank: Int,
  @Json val price: Int,

  @Json val kit: ServerGarageKit,

  @Json val properties: List<ServerGarageItemProperty>
) : ServerGarageItem(
  id, index, GarageItemType.Kit,
  name, description,
  baseItemId
)

class ServerGarageItemPresent(
  id: String,
  index: Int,

  name: String,
  description: String,

  baseItemId: Int,
  @Json val previewResourceId: Int,

  @Json val rank: Int,
  @Json val price: Int,

  @Json val properties: List<ServerGarageItemProperty>
) : ServerGarageItem(
  id, index, GarageItemType.Present,
  name, description,
  baseItemId
)

class ServerGarageItemSubscription(
  id: String,
  index: Int,

  name: String,
  description: String,

  baseItemId: Int,
  @Json val previewResourceId: Int,

  @Json val rank: Int,
  @Json val price: Int,

  @Json val properties: List<ServerGarageItemProperty>
) : ServerGarageItem(
  id, index, GarageItemType.Subscription,
  name, description,
  baseItemId
)

data class ServerGarageKit(
  @Json val image: Int,
  @Json val discount: Int,
  @Json val items: List<ServerGarageKitItem>,
  @Json val isTimeless: Boolean,
  @Json val timeLeft: Int?
)

data class ServerGarageKitItem(
  @Json val count: Int,
  @Json val id: String
)

data class ServerGarageItemProperty(
  @Json val property: String,
  @Json val value: Any?,
  @Json val properties: List<ServerGarageItemProperty>?
)

data class ServerGarageItemModification(
  @Json val previewResourceId: Int,
  @Json val object3ds: Int,

  @Json val rank: Int,
  @Json val price: Int,

  @Json val properties: List<ServerGarageItemProperty>
)

interface IServerGarageUserItem {
  val marketItem: ServerGarageItem

  val mountName: String
    get() = "${marketItem.id}_m0"
}

interface IServerGarageUserItemWithModification : IServerGarageUserItem {
  var modificationIndex: Int
  val modification: ServerGarageItemModification

  override val mountName: String
    get() = "${marketItem.id}_m${modificationIndex}"
}

class ServerGarageUserItemWeapon(
  override val marketItem: ServerGarageItemWeapon,
  override var modificationIndex: Int
) : IServerGarageUserItemWithModification {
  override val modification: ServerGarageItemModification
    get() = marketItem.modifications[modificationIndex]!! // TODO(Assasans)
}

class ServerGarageUserItemHull(
  override val marketItem: ServerGarageItemHull,
  override var modificationIndex: Int
) : IServerGarageUserItemWithModification {
  override val modification: ServerGarageItemModification
    get() = marketItem.modifications[modificationIndex]!! // TODO(Assasans)
}

class ServerGarageUserItemPaint(
  override val marketItem: ServerGarageItemPaint
) : IServerGarageUserItem

class ServerGarageUserItemSupply(
  override val marketItem: ServerGarageItemSupply,
  var count: Int
) : IServerGarageUserItem

class ServerGarageUserItemSubscription(
  override val marketItem: ServerGarageItemSubscription,
  var startTime: Instant,
  var duration: Duration
) : IServerGarageUserItem
