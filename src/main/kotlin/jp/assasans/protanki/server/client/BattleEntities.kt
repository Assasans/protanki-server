package jp.assasans.protanki.server.client

import com.squareup.moshi.Json
import jp.assasans.protanki.server.math.Vector3

fun Vector3Data.toVector() = Vector3(x, y, z)
fun Vector3.toVectorData() = Vector3Data(x, y, z)

data class Vector3Data(
  @Json val x: Double,
  @Json val y: Double,
  @Json val z: Double
) {
  constructor() : this(0.0, 0.0, 0.0)
}

open class MoveData(
  @Json val physTime: Int,

  @Json val control: Int,
  @Json val specificationID: Int,

  @Json val position: Vector3Data,
  @Json val linearVelocity: Vector3Data,

  @Json val orientation: Vector3Data,
  @Json val angularVelocity: Vector3Data
)

open class FullMoveData(
  physTime: Int,

  control: Int,
  specificationID: Int,

  position: Vector3Data,
  linearVelocity: Vector3Data,

  orientation: Vector3Data,
  angularVelocity: Vector3Data,

  @Json val turretDirection: Double
)  : MoveData(
  physTime,
  control, specificationID,
  position, linearVelocity,
  orientation, angularVelocity
)

class ClientMoveData(
  @Json val tankId: String,

  physTime: Int,

  control: Int,
  specificationID: Int,

  position: Vector3Data,
  linearVelocity: Vector3Data,

  orientation: Vector3Data,
  angularVelocity: Vector3Data
) : MoveData(
  physTime,
  control, specificationID,
  position, linearVelocity,
  orientation, angularVelocity
)

class ClientFullMoveData(
  @Json val tankId: String,

  physTime: Int,

  control: Int,
  specificationID: Int,

  position: Vector3Data,
  linearVelocity: Vector3Data,

  orientation: Vector3Data,
  angularVelocity: Vector3Data,

  turretDirection: Double
) : FullMoveData(
  physTime,
  control, specificationID,
  position, linearVelocity,
  orientation, angularVelocity,
  turretDirection
)

data class UpdateSpectatorsListData(
  @Json val spects: List<String>
)
