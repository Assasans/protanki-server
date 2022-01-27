package jp.assasans.protanki.server.math

import kotlin.math.pow
import kotlin.math.sqrt

class Vector3 {
  var x: Double
  var y: Double
  var z: Double

  constructor() : this(0.0, 0.0, 0.0)
  constructor(x: Double, y: Double, z: Double) {
    this.x = x
    this.y = y
    this.z = z
  }

  val length: Double
    get() = sqrt(x.pow(2) + y.pow(2) + z.pow(2))

  override fun toString(): String = "${this::class.simpleName}($x, $y, $z)"
}

operator fun Vector3.unaryPlus() = Vector3(x, y, z)
operator fun Vector3.unaryMinus() = Vector3(-x, -y, -z)

operator fun Vector3.plus(another: Vector3) = Vector3(x + another.x, y + another.y, z + another.z)
operator fun Vector3.minus(another: Vector3) = Vector3(x - another.x, y - another.y, z - another.z)
operator fun Vector3.times(scale: Double) = Vector3(x * scale, y * scale, z * scale)
operator fun Vector3.div(scale: Double) = Vector3(x / scale, y / scale, z / scale)
