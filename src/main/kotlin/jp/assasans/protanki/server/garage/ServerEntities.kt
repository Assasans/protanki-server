package jp.assasans.protanki.server.garage

import kotlin.time.Duration
import com.squareup.moshi.Json
import kotlinx.datetime.Instant
import jp.assasans.protanki.server.client.WeaponVisual

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

  @Json override val modifications: Map<Int, ServerGarageItemWeaponModification>
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

  @Json override val modifications: Map<Int, ServerGarageItemHullModification>
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

interface IPhysics {}

class WeaponPhysics(
  @Json val turretRotationSpeed: Double,
  @Json val turretTurnAcceleration: Double
) : IPhysics

class HullPhysics(
  @Json val speed: Double,
  @Json val turnSpeed: Double,
  @Json val acceleration: Double,
  @Json val reverseAcceleration: Double,
  @Json val sideAcceleration: Double,
  @Json val turnAcceleration: Double,
  @Json val reverseTurnAcceleration: Double,
  @Json val damping: Int
) : IPhysics

abstract class ServerGarageItemModification(
  @Json val previewResourceId: Int,
  @Json val object3ds: Int,

  @Json val rank: Int,
  @Json val price: Int,

  @Json val properties: List<ServerGarageItemProperty>,

  @Json open val physics: IPhysics
)

class ServerGarageItemWeaponModification(
  previewResourceId: Int,
  object3ds: Int,
  rank: Int,
  price: Int,
  properties: List<ServerGarageItemProperty>,

  override val physics: WeaponPhysics = WeaponPhysics( // TODO(Assasans)
    turretRotationSpeed = 1.2473868164003472,
    turretTurnAcceleration = 1.3264502315156905
  ),

  @Json val visual: WeaponVisual?
) : ServerGarageItemModification(previewResourceId, object3ds, rank, price, properties, physics)

class ServerGarageItemHullModification(
  previewResourceId: Int,
  object3ds: Int,
  rank: Int,
  price: Int,
  properties: List<ServerGarageItemProperty>,

  override val physics: HullPhysics = HullPhysics( // TODO(Assasans)
    speed = 10.8,
    turnSpeed = 1.6388642,
    acceleration = 9.4,
    reverseAcceleration = 13.26,
    sideAcceleration = 17.27,
    turnAcceleration = 2.968107,
    reverseTurnAcceleration = 5.0204396,
    damping = 900
  )
) : ServerGarageItemModification(previewResourceId, object3ds, rank, price, properties, physics)

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
  override val modification: ServerGarageItemWeaponModification
    get() = marketItem.modifications[modificationIndex]!! // TODO(Assasans)
}

class ServerGarageUserItemHull(
  override val marketItem: ServerGarageItemHull,
  override var modificationIndex: Int
) : IServerGarageUserItemWithModification {
  override val modification: ServerGarageItemHullModification
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
