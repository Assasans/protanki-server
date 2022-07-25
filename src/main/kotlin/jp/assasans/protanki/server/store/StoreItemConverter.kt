package jp.assasans.protanki.server.store

import jp.assasans.protanki.server.utils.toClientLocalizedString

interface IStoreItemConverter {
  fun toClientCategory(category: ServerStoreCategory): StoreCategory
  fun toClientItem(item: ServerStoreItem): StoreItem
}

class StoreItemConverter : IStoreItemConverter {
  override fun toClientCategory(category: ServerStoreCategory): StoreCategory {
    return StoreCategory(
      category_id = category.id,
      header_text = category.title.localized.toClientLocalizedString(),
      description = category.description.localized.toClientLocalizedString()
    )
  }

  override fun toClientItem(item: ServerStoreItem): StoreItem {
    if(item.crystals == null && item.premium == null) throw IllegalStateException("Item ${item.id} is neither a crystal nor a premium package")
    if(item.crystals != null && item.premium != null) throw IllegalStateException("Item ${item.id} cannot be both a crystal and a premium package")

    return StoreItem(
      category_id = item.category.id,
      item_id = item.id,
      properties = StoreItemProperties(
        price = item.price[StoreCurrency.JPY]!!, // TODO(Assasans)
        currency = StoreCurrency.JPY.displayName, // TODO(Assasans)

        crystals = item.crystals?.base,
        bonusCrystals = item.crystals?.bonus,

        premiumDuration = item.premium?.base
      )
    )
  }
}
