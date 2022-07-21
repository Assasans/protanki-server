package jp.assasans.protanki.server.client.weapons.twins

import com.squareup.moshi.Json
import jp.assasans.protanki.server.client.Vector3Data

open class Fire(
  @Json val physTime: Int,
  @Json val shotDirection: Vector3Data,
  @Json val currentBarrel: Int,
  @Json val shotId: Int
)

open class FireStatic(
  @Json val physTime: Int,
  @Json val hitPoint: Vector3Data,
  @Json(name = "currentBarrel") val shotId: Int
)

open class FireTarget(
  @Json val physTime: Int,
  @Json val target: String,

  @Json val targetPosition: Vector3Data,
  @Json val hitPoint: Vector3Data,

  @Json val shotId: Int
)
