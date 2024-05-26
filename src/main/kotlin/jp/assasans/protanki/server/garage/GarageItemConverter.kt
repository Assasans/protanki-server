package jp.assasans.protanki.server.garage

import jp.assasans.protanki.server.client.SocketLocale

interface IGarageItemConverter {
  fun toClientWeapon(item: ServerGarageItemWeapon, locale: SocketLocale): List<GarageItem>
  fun toClientHull(item: ServerGarageItemHull, locale: SocketLocale): List<GarageItem>
  fun toClientPaint(item: ServerGarageItemPaint, locale: SocketLocale): GarageItem
  fun toClientSupply(item: ServerGarageItemSupply, userItem: ServerGarageUserItemSupply?, locale: SocketLocale): GarageItem
  fun toClientSubscription(item: ServerGarageItemSubscription, userItem: ServerGarageUserItemSubscription?, locale: SocketLocale): GarageItem
  fun toClientKit(item: ServerGarageItemKit, locale: SocketLocale): GarageItem
  fun toClientPresent(item: ServerGarageItemPresent, locale: SocketLocale): GarageItem
}

class GarageItemConverter : IGarageItemConverter {
  private fun toClientProperties(properties: List<ServerGarageItemProperty>): List<GarageItemProperty> {
    return properties.map { property ->
      GarageItemProperty(
        property = property.property,
        value = if(property.value != null) property.value.toString() else null,
        subproperties = if(property.properties != null) toClientProperties(property.properties) else null
      )
    }
  }

  override fun toClientWeapon(item: ServerGarageItemWeapon, locale: SocketLocale): List<GarageItem> {
    return item.modifications.map { (index, modification) ->
      val nextModification = item.modifications.getOrDefault(index + 1, null)

      GarageItem(
        id = item.id,
        index = item.index,
        type = item.type,
        category = item.type.categoryKey,
        isInventory = false,

        name = item.name.get(locale),
        description = item.description.get(locale),

        baseItemId = item.baseItemId,
        previewResourceId = modification.previewResourceId,

        rank = modification.rank,
        next_rank = nextModification?.rank ?: modification.rank,

        price = modification.price,
        next_price = nextModification?.price ?: modification.price,
        discount = Discount(
          percent = 0,
          timeLeftInSeconds = -1,
          timeToStartInSeconds = -1
        ),

        timeLeft = -1,

        properties = toClientProperties(modification.properties),

        modificationID = index,
        object3ds = modification.object3ds,

        coloring = null,

        count = null,

        kit = null
      )
    }
  }

  override fun toClientHull(item: ServerGarageItemHull, locale: SocketLocale): List<GarageItem> {
    return item.modifications.map { (index, modification) ->
      val nextModification = item.modifications.getOrDefault(index + 1, null)

      GarageItem(
        id = item.id,
        index = item.index,
        type = item.type,
        category = item.type.categoryKey,
        isInventory = false,

        name = item.name.get(locale),
        description = item.description.get(locale),

        baseItemId = item.baseItemId,
        previewResourceId = modification.previewResourceId,

        rank = modification.rank,
        next_rank = nextModification?.rank ?: modification.rank,

        price = modification.price,
        next_price = nextModification?.price ?: modification.price,
        discount = Discount(
          percent = 0,
          timeLeftInSeconds = -1,
          timeToStartInSeconds = -1
        ),

        timeLeft = -1,

        properties = toClientProperties(modification.properties),

        modificationID = index,
        object3ds = modification.object3ds,

        coloring = null,

        count = null,

        kit = null
      )
    }
  }

  override fun toClientPaint(item: ServerGarageItemPaint, locale: SocketLocale): GarageItem {
    return GarageItem(
      id = item.id,
      index = item.index,
      type = item.type,
      category = item.type.categoryKey,
      isInventory = false,

      name = item.name.get(locale),
      description = item.description.get(locale),

      baseItemId = item.baseItemId,
      previewResourceId = item.previewResourceId,

      rank = item.rank,
      next_rank = item.rank,

      price = item.price,
      next_price = item.price,
      discount = Discount(
        percent = 0,
        timeLeftInSeconds = -1,
        timeToStartInSeconds = -1
      ),

      timeLeft = -1,

      properties = toClientProperties(item.properties),

      modificationID = null,
      object3ds = null,

      coloring = item.coloring,

      count = null,

      kit = null
    )
  }

  override fun toClientSupply(item: ServerGarageItemSupply, userItem: ServerGarageUserItemSupply?, locale: SocketLocale): GarageItem {
    return GarageItem(
      id = item.id,
      index = item.index,
      type = item.type,
      category = item.type.categoryKey,
      isInventory = true,

      name = item.name.get(locale),
      description = item.description.get(locale),

      baseItemId = item.baseItemId,
      previewResourceId = item.previewResourceId,

      rank = item.rank,
      next_rank = item.rank,

      price = item.price,
      next_price = item.price,
      discount = Discount(
        percent = 0,
        timeLeftInSeconds = -1,
        timeToStartInSeconds = -1
      ),

      timeLeft = -1,

      properties = toClientProperties(item.properties),

      modificationID = null,
      object3ds = null,

      coloring = null,

      count = userItem?.count,

      kit = null
    )
  }

  override fun toClientSubscription(item: ServerGarageItemSubscription, userItem: ServerGarageUserItemSubscription?, locale: SocketLocale): GarageItem {
    return GarageItem(
      id = item.id,
      index = item.index,
      type = item.type,
      category = item.type.categoryKey,
      isInventory = true,

      name = item.name.get(locale),
      description = item.description.get(locale),

      baseItemId = item.baseItemId,
      previewResourceId = item.previewResourceId,

      rank = item.rank,
      next_rank = item.rank,

      price = item.price,
      next_price = 0,
      discount = Discount(
        percent = 0,
        timeLeftInSeconds = -1,
        timeToStartInSeconds = -1
      ),

      timeLeft = userItem?.timeLeft?.inWholeSeconds ?: -1,

      properties = toClientProperties(item.properties),

      modificationID = null,
      object3ds = null,

      coloring = null,

      count = null,

      kit = null
    )
  }

  override fun toClientKit(item: ServerGarageItemKit, locale: SocketLocale): GarageItem {
    return GarageItem(
      id = item.id,
      index = item.index,
      type = item.type,
      category = item.type.categoryKey,
      isInventory = true,

      name = item.name.get(locale),
      description = item.description.get(locale),

      baseItemId = item.baseItemId,
      previewResourceId = item.previewResourceId,

      rank = item.rank,
      next_rank = item.rank,

      price = item.price,
      next_price = 0,
      discount = Discount(
        percent = 0,
        timeLeftInSeconds = -1,
        timeToStartInSeconds = -1
      ),

      timeLeft = -1,

      properties = toClientProperties(item.properties),

      modificationID = null,
      object3ds = null,

      coloring = null,

      count = null,

      kit = GarageItemKit(
        image = item.kit.image,
        discountInPercent = item.kit.discount,
        kitItems = item.kit.items.map { kitItem -> GarageItemKitItem(count = kitItem.count, id = kitItem.id) },
        isTimeless = item.kit.isTimeless,
        timeLeftInSeconds = if(!item.kit.isTimeless) item.kit.timeLeft ?: throw Exception("Kit time left is not set") else 0
      )
    )
  }

  override fun toClientPresent(item: ServerGarageItemPresent, locale: SocketLocale): GarageItem {
    return GarageItem(
      id = item.id,
      index = item.index,
      type = item.type,
      category = item.type.categoryKey,
      isInventory = false,

      name = item.name.get(locale),
      description = item.description.get(locale),

      baseItemId = item.baseItemId,
      previewResourceId = item.previewResourceId,

      rank = item.rank,
      next_rank = item.rank,

      price = item.price,
      next_price = item.price,
      discount = Discount(
        percent = 0,
        timeLeftInSeconds = -1,
        timeToStartInSeconds = -1
      ),

      timeLeft = -1,

      properties = toClientProperties(item.properties),

      modificationID = null,
      object3ds = null,

      coloring = null,

      count = null,

      kit = null
    )
  }
}
