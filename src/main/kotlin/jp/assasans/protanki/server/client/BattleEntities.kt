package jp.assasans.protanki.server.client

import com.squareup.moshi.Json

data class Vector3Data(
  @Json val x: Double,
  @Json val y: Double,
  @Json val z: Double
)

data class MoveData(
  @Json val physTime: Int,

  @Json val control: Int,
  @Json val specificationID: Int,

  @Json val position: Vector3Data,
  @Json val linearVelocity: Vector3Data,

  @Json val orientation: Vector3Data,
  @Json val angularVelocity: Vector3Data
)

data class FullMoveData(
  @Json val physTime: Int,

  @Json val control: Int,
  @Json val specificationID: Int,

  @Json val position: Vector3Data,
  @Json val linearVelocity: Vector3Data,

  @Json val orientation: Vector3Data,
  @Json val angularVelocity: Vector3Data
)
