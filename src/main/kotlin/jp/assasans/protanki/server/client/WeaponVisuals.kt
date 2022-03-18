package jp.assasans.protanki.server.client

import com.squareup.moshi.Json

data class WeaponLightning(
  @Json val attenuationBegin: Int,
  @Json val attenuationEnd: Int,
  @Json val color: Int,
  @Json val intensity: Double,
  @Json val time: Int
)

data class WeaponLightningGroup(
  @Json val name: String,
  @Json val light: List<WeaponLightning>
)

data class WeaponVisualColor(
  @Json val key: String,
  @Json val brightness: Double,
  @Json val contrast: Double,
  @Json val saturation: Double,
  @Json val hue: Double
)

data class WeaponVisualColorTransform(
  @Json val redMultiplier: Double,
  @Json val greenMultiplier: Double,
  @Json val blueMultiplier: Double,
  @Json val alphaMultiplier: Double,
  @Json val redOffset: Double,
  @Json val greenOffset: Double,
  @Json val blueOffset: Double,
  @Json val alphaOffset: Double,
  @Json val t: Double
)

abstract class WeaponVisual(
  @Json val lighting: List<WeaponLightningGroup>,
  @Json val bcsh: List<WeaponVisualColor>
) {
  @Json(name = "\$type") lateinit var type: String
}

class SmokyVisual(
  @Json val criticalHitSize: Int,
  @Json val criticalHitTexture: Int,

  @Json val explosionMarkTexture: Int,
  @Json val explosionSize: Int,
  @Json val explosionSound: Int,
  @Json val explosionTexture: Int,

  @Json val shotSound: Int,
  @Json val shotTexture: Int,

  lighting: List<WeaponLightningGroup>,
  bcsh: List<WeaponVisualColor>
) : WeaponVisual(lighting, bcsh)

class RailgunVisual(
  @Json val chargingPart1: Int,
  @Json val chargingPart2: Int,
  @Json val chargingPart3: Int,

  @Json val hitMarkTexture: Int,
  @Json val powTexture: Int,
  @Json val ringsTexture: Int,
  @Json val shotSound: Int,
  @Json val smokeImage: Int,
  @Json val sphereTexture: Int,
  @Json val trailImage: Int,

  lighting: List<WeaponLightningGroup>,
  bcsh: List<WeaponVisualColor>
) : WeaponVisual(lighting, bcsh)

class ThunderVisual(
  @Json val explosionMarkTexture: Int,
  @Json val explosionSize: Int,
  @Json val explosionSound: Int,
  @Json val explosionTexture: Int,

  @Json val shotSound: Int,
  @Json val shotTexture: Int,

  lighting: List<WeaponLightningGroup>,
  bcsh: List<WeaponVisualColor>
) : WeaponVisual(lighting, bcsh)

class FlamethrowerVisual(
  @Json val fireTexture: Int,
  @Json val flameSound: Int,

  @Json val muzzlePlaneTexture: Int,

  lighting: List<WeaponLightningGroup>,
  bcsh: List<WeaponVisualColor>,
  @Json val colorTransform: List<WeaponVisualColorTransform>
) : WeaponVisual(lighting, bcsh)

class FreezeVisual(
  @Json val particleSpeed: Int,
  @Json val particleTextureResource: Int,

  @Json val planeTextureResource: Int,
  @Json val shotSoundResource: Int,

  lighting: List<WeaponLightningGroup>,
  bcsh: List<WeaponVisualColor>
) : WeaponVisual(lighting, bcsh)

class IsidaVisual(
  @Json val damagingBall: Int,
  @Json val damagingRay: Int,
  @Json val damagingSound: Int,

  @Json val healingBall: Int,
  @Json val healingRay: Int,
  @Json val healingSound: Int,

  @Json val idleSound: Int,

  lighting: List<WeaponLightningGroup>,
  bcsh: List<WeaponVisualColor>
) : WeaponVisual(lighting, bcsh)

class TwinsVisual(
  @Json val explosionTexture: Int,
  @Json val hitMarkTexture: Int,
  @Json val muzzleFlashTexture: Int,

  @Json val shotSound: Int,
  @Json val shotTexture: Int,

  lighting: List<WeaponLightningGroup>,
  bcsh: List<WeaponVisualColor>
) : WeaponVisual(lighting, bcsh)

class ShaftVisual(
  @Json val explosionSound: Int,
  @Json val explosionTexture: Int,

  @Json val hitMarkTexture: Int,
  @Json val muzzleFlashTexture: Int,
  @Json val shotSound: Int,
  @Json val targetingSound: Int,
  @Json val trailTexture: Int,
  @Json val zoomModeSound: Int,

  lighting: List<WeaponLightningGroup>,
  bcsh: List<WeaponVisualColor>
) : WeaponVisual(lighting, bcsh)

class RicochetVisual(
  @Json val bumpFlashTexture: Int,

  @Json val explosionTexture: Int,
  @Json val explosionSound: Int,

  @Json val ricochetSound: Int,

  @Json val shotFlashTexture: Int,
  @Json val shotSound: Int,
  @Json val shotTexture: Int,

  @Json val tailTrailTexutre: Int,

  lighting: List<WeaponLightningGroup>,
  bcsh: List<WeaponVisualColor>
) : WeaponVisual(lighting, bcsh)
