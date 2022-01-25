package jp.assasans.protanki.server.math

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

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

  val length: Double
    get() = sqrt(w.pow(2) + x.pow(2) + y.pow(2) + z.pow(2))

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

  override fun toString(): String = "${Quaternion::class.simpleName}($w, $x, $y, $z)"
}

operator fun Quaternion.unaryPlus() = Quaternion(w, x, y, z)
operator fun Quaternion.unaryMinus() = Quaternion(w, -x, -y, -z)

operator fun Quaternion.plus(another: Quaternion) =
  Quaternion(w + another.w, x + another.x, y + another.y, z + another.z)

operator fun Quaternion.minus(another: Quaternion) =
  Quaternion(w - another.w, x - another.x, y - another.y, z - another.z)

operator fun Quaternion.times(scale: Double) = Quaternion(w * scale, x * scale, y * scale, z * scale)
operator fun Quaternion.div(scale: Double) = Quaternion(w / scale, x / scale, y / scale, z / scale)
