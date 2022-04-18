package jp.assasans.protanki.server.math

import java.math.RoundingMode
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

  fun copyFrom(another: Vector3) {
    x = another.x
    y = another.y
    z = another.z
  }

  val length: Double
    get() = sqrt(x.pow(2) + y.pow(2) + z.pow(2))

  override fun toString(): String {
    val builder = StringBuilder()
    builder.append("${this::class.simpleName}(")
    builder.append(x.toBigDecimal().setScale(3, RoundingMode.HALF_UP).toDouble())
    builder.append(", ")
    builder.append(y.toBigDecimal().setScale(3, RoundingMode.HALF_UP).toDouble())
    builder.append(", ")
    builder.append(z.toBigDecimal().setScale(3, RoundingMode.HALF_UP).toDouble())
    builder.append(")")

    return builder.toString()
  }
}

operator fun Vector3.unaryPlus() = Vector3(x, y, z)
operator fun Vector3.unaryMinus() = Vector3(-x, -y, -z)

operator fun Vector3.plus(another: Vector3) = Vector3(x + another.x, y + another.y, z + another.z)
operator fun Vector3.minus(another: Vector3) = Vector3(x - another.x, y - another.y, z - another.z)
operator fun Vector3.times(scale: Double) = Vector3(x * scale, y * scale, z * scale)
operator fun Vector3.div(scale: Double) = Vector3(x / scale, y / scale, z / scale)
