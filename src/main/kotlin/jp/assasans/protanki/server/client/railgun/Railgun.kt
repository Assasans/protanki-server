package jp.assasans.protanki.server.client.railgun

import com.squareup.moshi.Json
import jp.assasans.protanki.server.client.Vector3Data

// Server

// Not used
open class FireDummy(
  @Json val physTime: Int
)

open class FireStart(
  @Json val physTime: Int
)

open class FireTarget(
  @Json val physTime: Int,

  @Json val targets: List<String>,
  @Json val incarnations: List<Int>?,

  @Json val staticHitPosition: Vector3Data?,

  @Json val targetPositions: List<Vector3Data>?,
  @Json val hitPositions: List<Vector3Data>
)

// Client

class ShotDummy(
  physTime: Int
) : FireDummy(physTime) {
  constructor(fire: FireDummy) : this(
    fire.physTime
  )
}

class ShotStart(
  physTime: Int
) : FireStart(physTime) {
  constructor(fire: FireStart) : this(
    fire.physTime
  )
}

class ShotTarget(
  physTime: Int,

  targets: List<String>,
  incarnations: List<Int>?,

  staticHitPosition: Vector3Data?,

  targetPositions: List<Vector3Data>?,
  hitPositions: List<Vector3Data>
) : FireTarget(
  physTime,
  targets, incarnations,
  staticHitPosition,
  targetPositions, hitPositions
) {
  constructor(fire: FireTarget) : this(
    fire.physTime,
    fire.targets, fire.incarnations,
    fire.staticHitPosition,
    fire.targetPositions, fire.hitPositions
  )
}
