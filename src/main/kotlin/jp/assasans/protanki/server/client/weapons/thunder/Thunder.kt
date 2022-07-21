package jp.assasans.protanki.server.client.weapons.thunder

import com.squareup.moshi.Json
import jp.assasans.protanki.server.client.Vector3Data

open class Fire(
  @Json val physTime: Int
)

open class FireStatic(
  @Json val physTime: Int,

  @Json val hitPoint: Vector3Data,

  @Json val splashTargetIds: List<String>,
  @Json val splashTargetDistances: List<String>
)

data class FireTarget(
  @Json val physTime: Int,

  @Json val target: String,
  @Json val targetIncarnation: Int,
  @Json val targetPosition: Vector3Data,

  @Json val relativeHitPoint: Vector3Data,
  @Json val hitPointWorld: Vector3Data,

  @Json val splashTargetIds: List<String>,
  @Json val splashTargetDistances: List<String>
)
