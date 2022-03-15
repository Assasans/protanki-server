package jp.assasans.protanki.server.garage

interface IGarageItemConverter {
  fun toClientWeapon(item: ServerGarageItemWeapon): List<GarageItem>
  fun toClientHull(item: ServerGarageItemHull): List<GarageItem>
  fun toClientPaint(item: ServerGarageItemPaint): GarageItem
  fun toClientSupply(item: ServerGarageItemSupply): GarageItem
  fun toClientSubscription(item: ServerGarageItemSubscription): GarageItem
  fun toClientKit(item: ServerGarageItemKit): GarageItem
  fun toClientPresent(item: ServerGarageItemPresent): GarageItem
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

  override fun toClientWeapon(item: ServerGarageItemWeapon): List<GarageItem> {
    return item.modifications.map { (index, modification) ->
      val nextModification = item.modifications.getOrDefault(index + 1, null)

      GarageItem(
        id = item.id,
        index = item.index,
        type = item.type,
        category = item.type.categoryKey,
        isInventory = false,

        name = item.name,
        description = item.description,

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

        remainingTimeInSec = -1,

        properties = toClientProperties(modification.properties),

        modificationID = index,
        object3ds = modification.object3ds,

        coloring = null,

        count = null,

        kit = null
      )
    }
  }

  override fun toClientHull(item: ServerGarageItemHull): List<GarageItem> {
    return item.modifications.map { (index, modification) ->
      val nextModification = item.modifications.getOrDefault(index + 1, null)

      GarageItem(
        id = item.id,
        index = item.index,
        type = item.type,
        category = item.type.categoryKey,
        isInventory = false,

        name = item.name,
        description = item.description,

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

        remainingTimeInSec = -1,

        properties = toClientProperties(modification.properties),

        modificationID = index,
        object3ds = modification.object3ds,

        coloring = null,

        count = null,

        kit = null
      )
    }
  }

  override fun toClientPaint(item: ServerGarageItemPaint): GarageItem {
    return GarageItem(
      id = item.id,
      index = item.index,
      type = item.type,
      category = item.type.categoryKey,
      isInventory = false,

      name = item.name,
      description = item.description,

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

      remainingTimeInSec = -1,

      properties = toClientProperties(item.properties),

      modificationID = null,
      object3ds = null,

      coloring = item.coloring,

      count = null,

      kit = null
    )
  }

  override fun toClientSupply(item: ServerGarageItemSupply): GarageItem {
    return GarageItem(
      id = item.id,
      index = item.index,
      type = item.type,
      category = item.type.categoryKey,
      isInventory = true,

      name = item.name,
      description = item.description,

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

      remainingTimeInSec = -1,

      properties = toClientProperties(item.properties),

      modificationID = null,
      object3ds = null,

      coloring = null,

      count = null, // TODO(Assasans): Set later

      kit = null
    )
  }

  override fun toClientSubscription(item: ServerGarageItemSubscription): GarageItem {
    return GarageItem(
      id = item.id,
      index = item.index,
      type = item.type,
      category = item.type.categoryKey,
      isInventory = true,

      name = item.name,
      description = item.description,

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

      remainingTimeInSec = 1000000000, // TODO(Assasans)

      properties = toClientProperties(item.properties),

      modificationID = null,
      object3ds = null,

      coloring = null,

      count = null,

      kit = null
    )
  }

  override fun toClientKit(item: ServerGarageItemKit): GarageItem {
    return GarageItem(
      id = item.id,
      index = item.index,
      type = item.type,
      category = item.type.categoryKey,
      isInventory = true,

      name = item.name,
      description = item.description,

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

      remainingTimeInSec = -1,

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

  override fun toClientPresent(item: ServerGarageItemPresent): GarageItem {
    return GarageItem(
      id = item.id,
      index = item.index,
      type = item.type,
      category = item.type.categoryKey,
      isInventory = false,

      name = item.name,
      description = item.description,

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

      remainingTimeInSec = -1,

      properties = toClientProperties(item.properties),

      modificationID = null,
      object3ds = null,

      coloring = null,

      count = null,

      kit = null
    )
  }
}
