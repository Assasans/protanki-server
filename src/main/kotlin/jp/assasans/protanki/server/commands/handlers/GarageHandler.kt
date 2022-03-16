package jp.assasans.protanki.server.commands.handlers

import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.client.UserSocket
import jp.assasans.protanki.server.client.send
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler
import jp.assasans.protanki.server.garage.*

/*
Switch to garage from battle:
-> switch_garage
<- change_layout_state [GARAGE]
<- unload_battle
-> i_exit_from_battle
<- init_messages
* load garage resources *
<- init_garage_items [{"items":[...]}]
-> get_garage_data
<- init_market [{"items":[...]}]
<- end_layout_switch [garage, garage]
<- init_mounted_item [hunter_m0, 227169]
<- init_mounted_item [railgun_m0, 906685]
<- init_mounted_item [green_m0, 966681]
*/

class GarageHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val marketRegistry by inject<IGarageMarketRegistry>()

  @CommandHandler(CommandName.TryMountPreviewItem)
  suspend fun tryMountPreviewItem(socket: UserSocket, item: String) {
    Command(CommandName.MountItem, listOf(item, false.toString())).send(socket)
  }

  @CommandHandler(CommandName.TryMountItem)
  suspend fun tryMountItem(socket: UserSocket, rawItem: String) {
    val user = socket.user ?: throw Exception("No User")

    val item = rawItem.substringBeforeLast("_")
    val marketItem = marketRegistry.get(item)
    val currentItem = user.items.singleOrNull { userItem -> userItem.marketItem.id == item }

    logger.debug { "Trying to mount ${marketItem.id}..." }

    if(currentItem == null) {
      logger.debug { "Player ${user.username} (${user.id}}) tried to mount not owned item: ${marketItem.id}" }
      return
    }

    when(currentItem) {
      is ServerGarageUserItemWeapon -> user.equipment.weapon = currentItem
      is ServerGarageUserItemHull   -> user.equipment.hull = currentItem
      is ServerGarageUserItemPaint  -> user.equipment.paint = currentItem

      else                          -> {
        logger.debug { "Player ${user.username} (${user.id}}) tried to mount invalid item: ${marketItem.id} (${currentItem::class.simpleName})" }
        return
      }
    }

    Command(CommandName.MountItem, listOf(currentItem.mountName, true.toString())).send(socket)
  }

  // TODO(Assasans): Code repeating
  @CommandHandler(CommandName.TryBuyItem)
  suspend fun tryBuyItem(socket: UserSocket, rawItem: String, count: Int) {
    val user = socket.user ?: throw Exception("No User")

    if(count < 1) {
      logger.debug { "Player ${user.username} (${user.id}}) tried to buy invalid count of items: $count" }
      return
    }

    val item = rawItem.substringBeforeLast("_")
    val marketItem = marketRegistry.get(item)
    var currentItem = user.items.singleOrNull { userItem -> userItem.marketItem.id == item }

    var isNewItem = false

    when(marketItem) {
      is ServerGarageItemWeapon -> {
        if(currentItem == null) {
          currentItem = ServerGarageUserItemWeapon(marketItem, 0)
          user.items.add(currentItem)
          isNewItem = true

          val price = currentItem.modification.price
          if(user.crystals < price) {
            logger.debug { "Player ${user.username} (${user.id}}) tried to buy item: ${marketItem.id} ($price crystals), but does not has enough crystals (user: ${user.crystals} crystals, delta: ${user.crystals - price} crystals)" }
            return
          }
          user.crystals -= price

          logger.debug { "Bought weapon ${marketItem.id} ($price crystals)" }
        }
      }

      is ServerGarageItemHull   -> {
        if(currentItem == null) {
          currentItem = ServerGarageUserItemHull(marketItem, 0)
          user.items.add(currentItem)
          isNewItem = true

          val price = currentItem.modification.price
          if(user.crystals < price) {
            logger.debug { "Player ${user.username} (${user.id}}) tried to buy item: ${marketItem.id} ($price crystals), but does not has enough crystals (user: ${user.crystals} crystals, delta: ${user.crystals - price} crystals)" }
            return
          }
          user.crystals -= price

          logger.debug { "Bought hull ${marketItem.id} ($price crystals)" }
        }
      }

      is ServerGarageItemPaint  -> {
        if(currentItem == null) {
          currentItem = ServerGarageUserItemPaint(marketItem)
          user.items.add(currentItem)
          isNewItem = true

          val price = currentItem.marketItem.price
          if(user.crystals < price) {
            logger.debug { "Player ${user.username} (${user.id}}) tried to buy item: ${marketItem.id} ($price crystals), but does not has enough crystals (user: ${user.crystals} crystals, delta: ${user.crystals - price} crystals)" }
            return
          }
          user.crystals -= price

          logger.debug { "Bought paint ${marketItem.id} ($price crystals)" }
        }
      }

      is ServerGarageItemSupply -> {
        if(currentItem == null) {
          currentItem = ServerGarageUserItemSupply(marketItem, count)
          user.items.add(currentItem)
        } else {
          val supplyItem = currentItem as ServerGarageUserItemSupply
          supplyItem.count += count
        }
        isNewItem = true

        val price = currentItem.marketItem.price * count
        if(user.crystals < price) {
          logger.debug { "Player ${user.username} (${user.id}}) tried to buy item: ${marketItem.id} ($price crystals), but does not has enough crystals (user: ${user.crystals} crystals, delta: ${user.crystals - price} crystals)" }
          return
        }
        user.crystals -= price

        logger.debug { "Bought supply ${marketItem.id} (count: $count, $price crystals)" }
      }

      else                      -> {
        // TODO(Assasans)
      }
    }

    if(!isNewItem && currentItem is IServerGarageUserItemWithModification) {
      if(currentItem.modificationIndex < 3) {
        val oldModification = currentItem.modificationIndex
        currentItem.modificationIndex++

        val price = currentItem.modification.price
        if(user.crystals < price) {
          logger.debug { "Player ${user.username} (${user.id}}) tried to buy item: ${marketItem.id} ($price crystals), but does not has enough crystals (user: ${user.crystals} crystals, delta: ${user.crystals - price} crystals)" }
          return
        }
        user.crystals -= price

        logger.debug { "Upgraded ${marketItem.id} modification: M${oldModification} -> M${currentItem.modificationIndex} ($price crystals)" }
      }
    }

    socket.updateCrystals()
  }
}
