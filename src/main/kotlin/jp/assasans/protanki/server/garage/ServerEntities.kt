package jp.assasans.protanki.server.garage

import java.io.Serializable
import kotlin.time.Duration
import com.squareup.moshi.Json
import jakarta.persistence.*
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlinx.datetime.Clock
import jp.assasans.protanki.server.client.User
import jp.assasans.protanki.server.client.WeaponVisual
import jp.assasans.protanki.server.utils.LocalizedString

open class ServerGarageItem(
  @Json val id: String,
  @Json val index: Int,
  @Json val type: GarageItemType,

  @Json val name: LocalizedString,
  @Json val description: LocalizedString,

  @Json val baseItemId: Int
)

interface IServerGarageItemWithModifications {
  val modifications: Map<Int, ServerGarageItemModification>
}

class ServerGarageItemWeapon(
  id: String,
  index: Int,

  name: LocalizedString,
  description: LocalizedString,

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

  name: LocalizedString,
  description: LocalizedString,

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

  name: LocalizedString,
  description: LocalizedString,

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

  name: LocalizedString,
  description: LocalizedString,

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

  name: LocalizedString,
  description: LocalizedString,

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

  name: LocalizedString,
  description: LocalizedString,

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

  name: LocalizedString,
  description: LocalizedString,

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
  @Json val turretTurnAcceleration: Double,
  @Json val impactForce: Double,
  @Json val kickback: Double
) : IPhysics

class HullPhysics(
  @Json val speed: Double,
  @Json val turnSpeed: Double,
  @Json val acceleration: Double,
  @Json val reverseAcceleration: Double,
  @Json val sideAcceleration: Double,
  @Json val turnAcceleration: Double,
  @Json val reverseTurnAcceleration: Double,
  @Json val damping: Int,
  @Json val mass: Int,
  @Json val power: Double
) : IPhysics

abstract class ServerGarageItemModification(
  @Json val previewResourceId: Int,
  @Json val object3ds: Int,

  @Json val rank: Int,
  @Json val price: Int,

  @Json val properties: List<ServerGarageItemProperty>,

  @Json open val physics: IPhysics
)

data class WeaponDamage(
  // One of the following
  @Json val discrete: Discrete? = null,
  @Json val stream: Stream? = null,

  @Json val fixed: Fixed? = null,
  @Json val range: Range? = null,

  @Json val heal: Heal? = null,

  @Json val weakening: Weakening? = null,

  @Json val splash: Splash? = null
) {
  class Discrete

  data class Stream(
    @Json val capacity: Int,
    @Json val charge: Int,
    @Json val discharge: Int,
    @Json val interval: Int
  )

  data class Fixed(
    @Json val value: Double
  )

  data class Range(
    @Json val from: Double,
    @Json val to: Double
  )

  data class Weakening(
    @Json val from: Double,
    @Json val to: Double,
    @Json val minimum: Double
  )

  data class Splash(
    @Json val from: Double,
    @Json val to: Double,
    @Json val radius: Double
  )

  data class Heal(
    // One of the following
    @Json val fixed: Fixed? = null,
    @Json val range: Range? = null
  )
}

class ServerGarageItemWeaponModification(
  previewResourceId: Int,
  object3ds: Int,
  rank: Int,
  price: Int,
  properties: List<ServerGarageItemProperty>,

  @Json val damage: WeaponDamage = WeaponDamage( // TODO(Assasans)
    discrete = WeaponDamage.Discrete(),
    fixed = WeaponDamage.Fixed(21.12)
  ),

  override val physics: WeaponPhysics = WeaponPhysics( // TODO(Assasans)
    turretRotationSpeed = 1.2473868164003472,
    turretTurnAcceleration = 1.3264502315156905,
    impactForce = 1.75,
    kickback = 1.4043
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
    damping = 900,
    mass = 1376,
    power = 9.4
  )
) : ServerGarageItemModification(previewResourceId, object3ds, rank, price, properties, physics)

@Embeddable
class ServerGarageItemId(
  @ManyToOne
  val user: User,
  val itemName: String
) : Serializable {
  override fun equals(other: Any?): Boolean {
    if(this === other) return true
    if(other !is ServerGarageItemId) return false

    if(user != other.user) return false
    if(itemName != other.itemName) return false

    return true
  }

  override fun hashCode(): Int {
    var result = user.hashCode()
    result = 31 * result + itemName.hashCode()
    return result
  }
}

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(
  name = "garage_items",
  indexes = [
    // Index(name = "idx_garage_items_type", columnList = "user, marketItem", unique = true),
  ]
)
abstract class ServerGarageUserItem(
  user: User,
  id: String,
) : KoinComponent {
  @EmbeddedId
  val id: ServerGarageItemId = ServerGarageItemId(user, id)

  @Transient
  protected final var marketRegistry: IGarageMarketRegistry = get()
    private set

  @get:Transient
  open val marketItem: ServerGarageItem
    get() = marketRegistry.get(id.itemName)

  open val mountName: String
    get() = "${marketItem.id}_m0"

  // JPA does not initialize transient fields
  @PostLoad
  fun postLoad() {
    marketRegistry = get()
  }
}

@Entity
abstract class ServerGarageUserItemWithModification(
  user: User,
  id: String,
  var modificationIndex: Int,
) : ServerGarageUserItem(user, id) {
  abstract val modification: ServerGarageItemModification

  override val mountName: String
    get() = "${marketItem.id}_m${modificationIndex}"
}

@Entity
@DiscriminatorValue("weapon")
class ServerGarageUserItemWeapon(
  user: User,
  id: String,
  modificationIndex: Int
) : ServerGarageUserItemWithModification(user, id, modificationIndex) {
  @get:Transient
  override val marketItem: ServerGarageItemWeapon
    get() = marketRegistry.get(id.itemName).cast()

  override val modification: ServerGarageItemWeaponModification
    get() = marketItem.modifications[modificationIndex]!! // TODO(Assasans)
}

@Entity
@DiscriminatorValue("hull")
class ServerGarageUserItemHull(
  user: User,
  id: String,
  modificationIndex: Int
) : ServerGarageUserItemWithModification(user, id, modificationIndex) {
  @get:Transient
  override val marketItem: ServerGarageItemHull
    get() = marketRegistry.get(id.itemName).cast()

  override val modification: ServerGarageItemHullModification
    get() = marketItem.modifications[modificationIndex]!! // TODO(Assasans)
}

@Entity
@DiscriminatorValue("paint")
class ServerGarageUserItemPaint(
  user: User,
  id: String,
) : ServerGarageUserItem(user, id) {
  @get:Transient
  override val marketItem: ServerGarageItemPaint
    get() {
      return marketRegistry.get(id.itemName).cast()
    }
}

@Entity
@DiscriminatorValue("supply")
class ServerGarageUserItemSupply(
  user: User,
  id: String,
  var count: Int
) : ServerGarageUserItem(user, id) {
  @get:Transient
  override val marketItem: ServerGarageItemSupply
    get() = marketRegistry.get(id.itemName).cast()
}

@Entity
@DiscriminatorValue("subscription")
class ServerGarageUserItemSubscription(
  user: User,
  id: String,
  @Convert(converter = InstantToLongConverter::class)
  var endTime: Instant
) : ServerGarageUserItem(user, id) {
  @get:Transient
  override val marketItem: ServerGarageItemSubscription
    get() = marketRegistry.get(id.itemName).cast()

  val timeLeft: Duration
    get() = endTime - Clock.System.now()
}

@Converter
class InstantToLongConverter : AttributeConverter<Instant, Long> {
  override fun convertToDatabaseColumn(instant: Instant?): Long? = instant?.toEpochMilliseconds()
  override fun convertToEntityAttribute(milliseconds: Long?): Instant? = milliseconds?.let { Instant.fromEpochMilliseconds(it) }
}
