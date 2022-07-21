package jp.assasans.protanki.server.client.weapons.ricochet

import com.squareup.moshi.Json
import jp.assasans.protanki.server.client.Vector3Data

open class Fire(
  @Json val physTime: Int,

  @Json val shotId: Int,
  @Json val shotDirectionX: Double,
  @Json val shotDirectionY: Double,
  @Json val shotDirectionZ: Double
)

// Not used
open class FireDummy(
  @Json val physTime: Int
)

// Not used
open class FireStatic(
  @Json val physTime: Int,

  @Json val shotId: Int,

  @Json val impactPoints: List<Vector3Data>
)

open class FireTarget(
  @Json val physTime: Int,

  @Json val target: String,

  @Json val shotId: Int,

  @Json val impactPoints: List<Vector3Data>,
  @Json val hitPosition: Vector3Data
)
