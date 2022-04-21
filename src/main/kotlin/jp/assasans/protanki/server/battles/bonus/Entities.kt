package jp.assasans.protanki.server.battles.bonus

import com.squareup.moshi.Json

data class SpawnBonusDatta(
  @Json val id: String,
  @Json val x: Double,
  @Json val y: Double,
  @Json val z: Double,
  @Json val disappearing_time: Int
)
