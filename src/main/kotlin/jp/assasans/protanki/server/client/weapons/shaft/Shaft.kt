package jp.assasans.protanki.server.client.weapons.shaft

import com.squareup.moshi.Json
import jp.assasans.protanki.server.client.Vector3Data

data class FireTarget(
  @Json val physTime: Int,

  @Json val target: String?,
  @Json val incarnation: Int,

  @Json val targetPosition: Vector3Data?,
  @Json val targetPositionGlobal: Vector3Data?,
  @Json val hitPoint: Vector3Data?,

  @Json val staticHitPosition: Vector3Data?
)

data class ShotTarget(
  @Json val physTime: Int,

  @Json val target: String?,
  @Json val incarnation: Int,

  @Json val targetPosition: Vector3Data?,
  @Json val targetPositionGlobal: Vector3Data?,
  @Json val hitPoint: Vector3Data?,

  @Json val staticHitPosition: Vector3Data?,

  @Json val impactForce: Double
) {
  constructor(
    fireTarget: FireTarget,
    impactForce: Double
  ) : this(
    fireTarget.physTime,
    fireTarget.target,
    fireTarget.incarnation,
    fireTarget.targetPosition,
    fireTarget.targetPositionGlobal,
    fireTarget.hitPoint,
    fireTarget.staticHitPosition,
    impactForce
  )
}
