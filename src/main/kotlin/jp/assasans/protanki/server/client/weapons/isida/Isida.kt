package jp.assasans.protanki.server.client.weapons.isida

import com.squareup.moshi.Json
import jp.assasans.protanki.server.client.Vector3Data

enum class IsidaFireMode(val key: String) {
  Damage("damage"),
  Heal("heal");

  companion object {
    private val map = values().associateBy(IsidaFireMode::key)

    fun get(key: String) = map[key]
  }
}

data class StartFire(
  @Json val physTime: Int,

  @Json val target: String,
  @Json val incarnation: Int,

  @Json val localHitPoint: Vector3Data
)

data class SetTarget(
  @Json val physTime: Int,

  @Json val target: String,
  @Json val incarnation: Int,
  @Json val actionType: IsidaFireMode,

  @Json val localHitPoint: Vector3Data
)

data class ResetTarget(
  @Json val physTime: Int
)
