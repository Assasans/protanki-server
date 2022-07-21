package jp.assasans.protanki.server.client.weapons.railgun

import com.squareup.moshi.Json
import jp.assasans.protanki.server.client.Vector3Data

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
