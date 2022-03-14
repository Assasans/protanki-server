package jp.assasans.protanki.server.garage

import kotlin.io.path.absolute
import kotlin.io.path.extension
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.readText
import com.squareup.moshi.Moshi
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.IResourceManager

class GarageMarketRegistry : KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val json by inject<Moshi>()
  private val resourceManager by inject<IResourceManager>()

  val items: MutableMap<GarageItemType, MutableMap<String, ServerGarageItem>> = mutableMapOf()

  inline fun <reified T : ServerGarageItem> get(type: GarageItemType, id: String): T {
    val item = items[type]!![id]!! // TODO(Assasans)
    if(item is T) return item
    throw Exception("Incompatible type: expected ${T::class.simpleName}, got ${item::class.simpleName}")
  }

  suspend fun load() {
    val typeDirectories = mapOf(
      Pair(GarageItemType.Weapon, Pair("weapons", ServerGarageItemWeapon::class)),
      Pair(GarageItemType.Hull, Pair("hulls", ServerGarageItemHull::class)),
      Pair(GarageItemType.Paint, Pair("paints", ServerGarageItemPaint::class)),
      Pair(GarageItemType.Supply, Pair("supplies", ServerGarageItemSupply::class)),
      Pair(GarageItemType.Subscription, Pair("subscriptions", ServerGarageItemSubscription::class)),
      Pair(GarageItemType.Kit, Pair("kits", ServerGarageItemKit::class)),
      Pair(GarageItemType.Present, Pair("presents", ServerGarageItemPresent::class))
    )

    for((type, pair) in typeDirectories) {
      val directory = pair.first
      val itemClass = pair.second

      logger.debug { "Loading garage item group ${type.name}..." }

      // TODO(Assasans): Shit
      resourceManager.get("garage/items/$directory").absolute().forEachDirectoryEntry { entry ->
        if(entry.extension != "json") return@forEachDirectoryEntry

        val item = json
          .adapter(itemClass.java)
          .failOnUnknown()
          .fromJson(entry.readText())!!

        items.getOrPut(type) { mutableMapOf() }[item.id] = item

        logger.debug { "  > Loaded garage item ${item.id} -> ${item.name}" }
      }
    }
  }
}
