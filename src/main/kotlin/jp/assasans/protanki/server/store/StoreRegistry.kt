package jp.assasans.protanki.server.store

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.io.path.*
import jp.assasans.protanki.server.IResourceManager
import jp.assasans.protanki.server.utils.LocalizedString

interface IStoreRegistry {
  val categories: MutableMap<String, ServerStoreCategory>

  suspend fun load()
}

class StoreRegistry : IStoreRegistry, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val json by inject<Moshi>()
  private val resourceManager by inject<IResourceManager>()

  override val categories: MutableMap<String, ServerStoreCategory> = mutableMapOf()

  override suspend fun load() {
    resourceManager.get("store/items").absolute().forEachDirectoryEntry category@{ categoryEntry ->
      logger.debug { "Loading store category ${categoryEntry.name}..." }

      val category = json
        .adapter(ServerStoreCategory::class.java)
        .failOnUnknown()
        .fromJson(categoryEntry.resolve("category.json").readText())!!
      category.id = categoryEntry.nameWithoutExtension
      category.items = mutableListOf()

      categoryEntry.forEachDirectoryEntry item@{ entry ->
        if(entry.name == "category.json") return@item
        if(entry.extension != "json") return@item

        val item = json
          .adapter(ServerStoreItem::class.java)
          .failOnUnknown()
          .fromJson(entry.readText())!!
        item.id = entry.nameWithoutExtension
        item.category = category

        category.items.add(item)

        logger.debug { "  > Loaded store item ${item.id}" }
      }

      categories[category.id] = category
    }
  }
}
