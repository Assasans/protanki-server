package jp.assasans.protanki.server.math

import java.math.RoundingMode
import kotlin.math.*

class Quaternion {
  var w: Double
  var x: Double
  var y: Double
  var z: Double

  constructor() : this(1.0, 0.0, 0.0, 0.0)
  constructor(w: Double, x: Double, y: Double, z: Double) {
    this.w = w
    this.x = x
    this.y = y
    this.z = z
  }

  fun copyFrom(another: Quaternion) {
    w = another.w
    x = another.x
    y = another.y
    z = another.z
  }

  fun toEulerAngles(): Vector3 {
    val qi2 = 2 * x * x
    val qj2 = 2 * y * y
    val qk2 = 2 * z * z
    val qij = 2 * x * y
    val qjk = 2 * y * z
    val qki = 2 * z * x
    val qri = 2 * w * x
    val qrj = 2 * w * y
    val qrk = 2 * w * z
    val aa = 1 - qj2 - qk2
    val bb = qij - qrk
    val ee = qij + qrk
    val ff = 1 - qi2 - qk2
    val ii = qki - qrj
    val jj = qjk + qri
    val kk = 1 - qi2 - qj2

    if(-1 < ii && ii < 1) return Vector3(atan2(jj, kk), -asin(ii), atan2(ee, aa))
    return Vector3(0.0, 0.5 * (if(ii <= -1) Math.PI else -Math.PI), atan2(-bb, ff))
  }

  fun fromEulerAngles(vector: Vector3) {
    val deg2Rad = PI * 2 / 360.0

    var yaw = vector.x
    var pitch = vector.y
    var roll = vector.z

    yaw *= deg2Rad
    pitch *= deg2Rad
    roll *= deg2Rad

    val rollOver2 = roll * 0.5f
    val sinRollOver2 = sin(rollOver2.toDouble())
    val cosRollOver2 = cos(rollOver2.toDouble())
    val pitchOver2 = pitch * 0.5f
    val sinPitchOver2 = sin(pitchOver2.toDouble())
    val cosPitchOver2 = cos(pitchOver2.toDouble())
    val yawOver2 = yaw * 0.5f
    val sinYawOver2 = sin(yawOver2.toDouble())
    val cosYawOver2 = cos(yawOver2.toDouble())

    w = (cosYawOver2 * cosPitchOver2 * cosRollOver2 + sinYawOver2 * sinPitchOver2 * sinRollOver2).toDouble()
    x = (cosYawOver2 * sinPitchOver2 * cosRollOver2 + sinYawOver2 * cosPitchOver2 * sinRollOver2).toDouble()
    y = (sinYawOver2 * cosPitchOver2 * cosRollOver2 - cosYawOver2 * sinPitchOver2 * sinRollOver2).toDouble()
    z = (cosYawOver2 * cosPitchOver2 * sinRollOver2 - sinYawOver2 * sinPitchOver2 * cosRollOver2).toDouble()
  }

  val length: Double
    get() = sqrt(w.pow(2) + x.pow(2) + y.pow(2) + z.pow(2))

  override fun toString(): String {
    val builder = StringBuilder()
    builder.append("${this::class.simpleName}(")
    builder.append(w.toBigDecimal().setScale(3, RoundingMode.HALF_UP).toDouble())
    builder.append(", ")
    builder.append(x.toBigDecimal().setScale(3, RoundingMode.HALF_UP).toDouble())
    builder.append(", ")
    builder.append(y.toBigDecimal().setScale(3, RoundingMode.HALF_UP).toDouble())
    builder.append(", ")
    builder.append(z.toBigDecimal().setScale(3, RoundingMode.HALF_UP).toDouble())
    builder.append(")")

    return builder.toString()
  }
}

operator fun Quaternion.unaryPlus() = Quaternion(w, x, y, z)
operator fun Quaternion.unaryMinus() = Quaternion(w, -x, -y, -z)

operator fun Quaternion.plus(another: Quaternion) =
  Quaternion(w + another.w, x + another.x, y + another.y, z + another.z)

operator fun Quaternion.minus(another: Quaternion) =
  Quaternion(w - another.w, x - another.x, y - another.y, z - another.z)

operator fun Quaternion.times(scale: Double) = Quaternion(w * scale, x * scale, y * scale, z * scale)
operator fun Quaternion.div(scale: Double) = Quaternion(w / scale, x / scale, y / scale, z / scale)
