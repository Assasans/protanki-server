package jp.assasans.protanki.server.api

import com.squareup.moshi.Json
import jp.assasans.protanki.server.client.Screen

data class PlayerStats(
  @Json val registered: Long,
  @Json val online: Int,
  @Json val screens: Map<Screen, Int>
)
