package jp.assasans.protanki.server.extensions

import kotlin.random.Random
import kotlin.random.asJavaRandom

fun Random.nextGaussianRange(from: Double, to: Double): Double {
  val mean = (from + to) / 2.0
  val deviation = (to - from) / 6.0
  return (mean + deviation * asJavaRandom().nextGaussian()).coerceIn(from, to)
}
