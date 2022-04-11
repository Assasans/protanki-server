package jp.assasans.protanki.server

import com.squareup.moshi.Json
import io.ktor.utils.io.bits.*
import jp.assasans.protanki.server.battles.BattleMode
import jp.assasans.protanki.server.battles.BattleTeam
import jp.assasans.protanki.server.client.Vector3Data
import jp.assasans.protanki.server.math.Vector3

interface IResourceConverter {
  fun toClientResource(resource: ServerResource): ClientResource
}

class ResourceConverter : IResourceConverter {
  override fun toClientResource(resource: ServerResource): ClientResource {
    return ClientResource(
      idhigh = resource.id.highInt,
      idlow = resource.id.lowInt,

      versionhigh = resource.version.highInt,
      versionlow = resource.version.lowInt,

      type = resource.type,
      lazy = resource.lazy,

      alpha = resource.alpha,

      weight = resource.width,
      height = resource.height,
      numFrames = resource.frames,
      fps = resource.fps
    )
  }
}

data class ServerMapFlag(
  @Json val position: ServerVector3
)

data class ServerMapDominationPoint(
  @Json val id: String,
  @Json val distance: Double,
  @Json val free: Boolean,
  @Json val position: Vector3Data
)

data class ServerMapInfo(
  @Json val name: String,
  @Json val theme: ServerMapTheme,

  @Json val id: Int,
  @Json val preview: Int,

  @Json val visual: ServerMapVisual,
  @Json val skybox: ServerMapSkybox,
  @Json val resources: ServerMapResources,

  @Json val spawnPoints: List<ServerMapSpawnPoint>,
  @Json val flags: Map<BattleTeam, ServerMapFlag>?,
  @Json val points: List<ServerMapDominationPoint>?
)

fun ServerVector3.toVector() = Vector3(x, y, z)
fun Vector3.toServerVector() = ServerVector3(x, y, z)

data class ServerVector3(
  val x: Double,
  val y: Double,
  val z: Double
)

data class ServerMapSpawnPoint(
  @Json val mode: BattleMode?,
  @Json val team: BattleTeam?,
  @Json val point: String?, // CP point name
  @Json val position: ServerVector3,
  @Json val rotation: ServerVector3
)

data class ServerMapSkybox(
  @Json val top: Int,
  @Json val front: Int,
  @Json val back: Int,
  @Json val bottom: Int,
  @Json val left: Int,
  @Json val right: Int
)

data class ServerMapVisual(
  @Json val angleX: Double,
  @Json val angleZ: Double,

  @Json val lightColor: Int,
  @Json val shadowColor: Int,

  @Json val fogAlpha: Double,
  @Json val fogColor: Int,

  @Json val farLimit: Int,
  @Json val nearLimit: Int,

  @Json val gravity: Int,
  @Json val skyboxRevolutionSpeed: Double,
  @Json val ssaoColor: Int,

  @Json val dustAlpha: Double,
  @Json val dustDensity: Double,
  @Json val dustFarDistance: Int,
  @Json val dustNearDistance: Int,
  @Json val dustParticle: String,
  @Json val dustSize: Int
)

data class ServerMapResources(
  @Json val props: List<ServerResource>,
  @Json val skybox: List<ServerResource>,
  @Json val map: List<ServerResource>
)

enum class ServerMapTheme(val key: String, val clientKey: String, val visualKey: String) {
  SummerDay("summer_day", clientKey = "SUMMER", visualKey = "SUMMER"),
  SummerNight("summer_night", clientKey = "SUMMER_NIGHT", visualKey = "SUMMER_NIGHT"),
  Winter("winter_day", clientKey = "WINTER", visualKey = "WINTER"),
  Space("space", clientKey = "SPACE", visualKey = "SPACE");

  companion object {
    private val map = values().associateBy(ServerMapTheme::key)
    private val mapByClient = values().associateBy(ServerMapTheme::clientKey)

    fun get(key: String) = map[key]
    fun getByClient(key: String) = mapByClient[key]
  }
}

enum class ResourceType(val key: Int) {
  SwfLibrary(1),
  ModelAlternativa3D(2),
  MovieClip(3),
  Sound(4),
  Map(7),
  PropLibrary(8),
  LegacyModel3DS(9), // Not used
  Image(10),
  MultiframeImage(11),
  LocalizedImage(13),
  Model3DS(17);

  companion object {
    private val map = values().associateBy(ResourceType::key)

    fun get(key: Int) = map[key]
  }
}

data class ServerResource(
  @Json val id: Long,
  @Json val version: Long,

  @Json val type: ResourceType,
  @Json val lazy: Boolean,

  @Json val alpha: Boolean?,

  @Json val width: Int?,
  @Json val height: Int?,
  @Json val frames: Int?,
  @Json val fps: Int?
)

/**
 * @remarks Original server sends high bits of Long values as String, but client implicitly parses them as Int
 */
data class ClientResource(
  @Json val idhigh: Int,
  @Json val idlow: Int,

  @Json val versionhigh: Int,
  @Json val versionlow: Int,

  @Json val type: ResourceType,
  @Json val lazy: Boolean,

  @Json val alpha: Boolean?,

  @Json val weight: Int?,
  @Json val height: Int?,
  @Json val numFrames: Int?,
  @Json val fps: Int?
)

data class ClientResources(
  @Json val resources: List<ClientResource>
)
