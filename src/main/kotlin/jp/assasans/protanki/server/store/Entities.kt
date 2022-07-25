package jp.assasans.protanki.server.store

import com.squareup.moshi.Json
import jp.assasans.protanki.server.utils.LocalizedString

data class ServerStoreCategory(
  @Json val title: LocalizedString,
  @Json val description: LocalizedString
) {
  lateinit var id: String
  lateinit var items: MutableList<ServerStoreItem>
}

data class ServerStoreItem(
  val price: Map<StoreCurrency, Double>,

  // One of the following
  val crystals: CrystalsPackage? = null,
  val premium: PremiumPackage? = null
) {
  lateinit var id: String
  lateinit var category: ServerStoreCategory

  data class CrystalsPackage(
    @Json val base: Int,
    @Json val bonus: Int = 0
  )

  data class PremiumPackage(
    @Json val base: Int
  )
}
