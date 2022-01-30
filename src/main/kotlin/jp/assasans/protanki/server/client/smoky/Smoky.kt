package jp.assasans.protanki.server.client.smoky

import com.squareup.moshi.Json
import jp.assasans.protanki.server.client.Vector3Data

// Server

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

// Client

class Shot(
  physTime: Int
) : Fire(physTime) {
  constructor(fire: Fire) : this(
    fire.physTime
  )
}

class StaticShot(
  physTime: Int,
  hitPosition: Vector3Data
) : FireStatic(physTime, hitPosition) {
  constructor(fire: FireStatic) : this(
    fire.physTime,
    fire.hitPosition
  )
}

class ShotTarget(
  physTime: Int,

  target: String,
  incration: Int,

  targetPosition: Vector3Data,
  hitPosition: Vector3Data,

  @Json val weakening: Double,
  @Json val critical: Boolean
) : FireTarget(
  physTime,
  target, incration,
  targetPosition, hitPosition
) {
  constructor(fire: FireTarget, weakening: Double, critical: Boolean) : this(
    fire.physTime,
    fire.target, fire.incration,
    fire.targetPosition, fire.hitPosition,
    weakening,
    critical
  )
}
