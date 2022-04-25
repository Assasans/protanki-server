package jp.assasans.protanki.server.battles.map

import kotlin.io.path.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.IResourceManager
import jp.assasans.protanki.server.ServerMapInfo
import jp.assasans.protanki.server.ServerMapTheme
import jp.assasans.protanki.server.ServerProplib

interface IMapRegistry {
  val proplibs: MutableList<ServerProplib>
  val maps: MutableList<ServerMapInfo>

  suspend fun load()
}

class MapRegistry : IMapRegistry, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val json by inject<Moshi>()
  private val resourceManager by inject<IResourceManager>()

  override val proplibs: MutableList<ServerProplib> = mutableListOf()
  override val maps: MutableList<ServerMapInfo> = mutableListOf()

  override suspend fun load() {
    logger.debug { "Loading proplibs..." }
    resourceManager.get("proplibs.json").let { entry ->
      val proplibs = json
        .adapter<List<ServerProplib>>(Types.newParameterizedType(List::class.java, ServerProplib::class.java))
        .failOnUnknown()
        .fromJson(entry.readText())!!

      proplibs.forEach { proplib ->
        this.proplibs.add(proplib)
        logger.debug { "  > Loaded proplib $proplib" }
      }
    }

    resourceManager.get("maps/").absolute().forEachDirectoryEntry { group ->
      if(!group.isDirectory()) return

      logger.debug { "Loading map group ${group.name}..." }

      group.forEachDirectoryEntry { entry ->
        if(!entry.isRegularFile()) return

        val map = json
          .adapter(ServerMapInfo::class.java)
          .failOnUnknown()
          .fromJson(entry.readText())!!

        maps.add(map)

        logger.debug { "  > Loaded map ${entry.name} -> ${map.name}@${map.theme.name} (ID: ${map.id}, preview: ${map.preview})" }
      }
    }
  }
}

fun IMapRegistry.get(name: String, theme: ServerMapTheme): ServerMapInfo {
  return maps.single { map -> map.name == name && map.theme == theme }
}

fun IMapRegistry.getProplib(name: String): ServerProplib {
  return proplibs.single { map -> map.name == name }
}
