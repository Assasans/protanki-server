package jp.assasans.protanki.server.client.weapons.smoky

import com.squareup.moshi.Json
import jp.assasans.protanki.server.client.Vector3Data

open class Fire(
  @Json val physTime: Int
)

open class FireStatic(
  @Json val physTime: Int,
  @Json val hitPosition: Vector3Data
)

open class FireTarget(
  @Json val physTime: Int,

  @Json val target: String,
  @Json val incration: Int,

  @Json val targetPosition: Vector3Data,
  @Json val hitPosition: Vector3Data
)

open class ShotTarget(
  @Json val physTime: Int,

  @Json val target: String,
  @Json val incration: Int,

  @Json val targetPosition: Vector3Data,
  @Json val hitPosition: Vector3Data,

  @Json val weakening: Double,
  @Json val critical: Boolean
) {
  constructor(
    fireTarget: FireTarget,
    weakening: Double,
    critical: Boolean
  ) : this(
    fireTarget.physTime,
    fireTarget.target,
    fireTarget.incration,
    fireTarget.targetPosition,
    fireTarget.hitPosition,
    weakening,
    critical
  )
}
