package jp.assasans.protanki.server.commands.handlers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.HibernateUtils
import jp.assasans.protanki.server.battles.BattleProperty
import jp.assasans.protanki.server.client.*
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
  private val userRepository by inject<IUserRepository>()

  @CommandHandler(CommandName.TryMountPreviewItem)
  suspend fun tryMountPreviewItem(socket: UserSocket, item: String) {
    Command(CommandName.MountItem, item, false.toString()).send(socket)
  }

  @CommandHandler(CommandName.TryMountItem)
  suspend fun tryMountItem(socket: UserSocket, rawItem: String) {
    val user = socket.user ?: throw Exception("No User")

    val item = rawItem.substringBeforeLast("_")
    val marketItem = marketRegistry.get(item)
    val currentItem = user.items.singleOrNull { userItem -> userItem.marketItem.id == item }

    logger.debug { "Trying to mount ${marketItem.id}..." }

    if(currentItem == null) {
      logger.debug { "Player ${user.username} (${user.id}) tried to mount not owned item: ${marketItem.id}" }
      return
    }

    val entityManager = HibernateUtils.createEntityManager()
    entityManager.transaction.begin()

    when(currentItem) {
      is ServerGarageUserItemWeapon -> user.equipment.weapon = currentItem
      is ServerGarageUserItemHull   -> user.equipment.hull = currentItem
      is ServerGarageUserItemPaint  -> user.equipment.paint = currentItem

      else                          -> {
        logger.debug { "Player ${user.username} (${user.id}) tried to mount invalid item: ${marketItem.id} (${currentItem::class.simpleName})" }
        return
      }
    }

    withContext(Dispatchers.IO) {
      entityManager
        .createQuery("UPDATE User SET equipment = :equipment WHERE id = :id")
        .setParameter("equipment", user.equipment)
        .setParameter("id", user.id)
        .executeUpdate()
    }
    entityManager.transaction.commit()
    entityManager.close()

    val player = socket.battlePlayer
    if(player != null) {
      if(!player.battle.properties[BattleProperty.RearmingEnabled]) {
        logger.warn { "Player ${player.user.username} attempted to change equipment in battle with disabled rearming" }
        return
      }

      player.equipmentChanged = true
    }

    Command(CommandName.MountItem, currentItem.mountName, true.toString()).send(socket)
  }

  // TODO(Assasans): Code repeating
  @CommandHandler(CommandName.TryBuyItem)
  suspend fun tryBuyItem(socket: UserSocket, rawItem: String, count: Int) {
    val user = socket.user ?: throw Exception("No User")

    if(count < 1) {
      logger.debug { "Player ${user.username} (${user.id}) tried to buy invalid count of items: $count" }
      return
    }

    val entityManager = HibernateUtils.createEntityManager()

    val item = rawItem.substringBeforeLast("_")
    val marketItem = marketRegistry.get(item)
    var currentItem = user.items.singleOrNull { userItem -> userItem.marketItem.id == item }

    var isNewItem = false

    entityManager.transaction.begin()

    when(marketItem) {
      is ServerGarageItemWeapon -> {
        if(currentItem == null) {
          currentItem = ServerGarageUserItemWeapon(user, marketItem.id, 0)
          user.items.add(currentItem)
          isNewItem = true

          val price = currentItem.modification.price
          if(user.crystals < price) {
            logger.debug { "Player ${user.username} (${user.id}) tried to buy item: ${marketItem.id} ($price crystals), but does not has enough crystals (user: ${user.crystals} crystals, delta: ${user.crystals - price} crystals)" }
            return
          }
          user.crystals -= price

          logger.debug { "Bought weapon ${marketItem.id} ($price crystals)" }
        }
      }

      is ServerGarageItemHull   -> {
        if(currentItem == null) {
          currentItem = ServerGarageUserItemHull(user, marketItem.id, 0)
          user.items.add(currentItem)
          isNewItem = true

          val price = currentItem.modification.price
          if(user.crystals < price) {
            logger.debug { "Player ${user.username} (${user.id}) tried to buy item: ${marketItem.id} ($price crystals), but does not has enough crystals (user: ${user.crystals} crystals, delta: ${user.crystals - price} crystals)" }
            return
          }
          user.crystals -= price

          logger.debug { "Bought hull ${marketItem.id} ($price crystals)" }
        }
      }

      is ServerGarageItemPaint  -> {
        if(currentItem == null) {
          currentItem = ServerGarageUserItemPaint(user, marketItem.id)
          user.items.add(currentItem)
          isNewItem = true

          val price = currentItem.marketItem.price
          if(user.crystals < price) {
            logger.debug { "Player ${user.username} (${user.id}) tried to buy item: ${marketItem.id} ($price crystals), but does not has enough crystals (user: ${user.crystals} crystals, delta: ${user.crystals - price} crystals)" }
            return
          }
          user.crystals -= price

          logger.debug { "Bought paint ${marketItem.id} ($price crystals)" }
        }
      }

      is ServerGarageItemSupply -> {
        when(marketItem.id) {
          "1000_scores" -> {
            user.score += 1000 * count
            socket.updateScore()

            val price = marketItem.price * count
            if(user.crystals < price) {
              logger.debug { "Player ${user.username} (${user.id}) tried to buy item: ${marketItem.id} ($price crystals), but does not has enough crystals (user: ${user.crystals} crystals, delta: ${user.crystals - price} crystals)" }
              return
            }
            user.crystals -= price

            logger.debug { "Bought ${marketItem.id} (count: $count, ${count * 1000} XP, $price crystals)" }
          }

          else          -> {
            if(currentItem == null) {
              currentItem = ServerGarageUserItemSupply(user, marketItem.id, count)
              user.items.add(currentItem)
              isNewItem = true
            } else {
              val supplyItem = currentItem as ServerGarageUserItemSupply
              supplyItem.count += count

              withContext(Dispatchers.IO) {
                entityManager
                  .createQuery("UPDATE ServerGarageUserItemSupply SET count = :count WHERE id = :id")
                  .setParameter("count", supplyItem.count)
                  .setParameter("id", supplyItem.id)
                  .executeUpdate()
              }
            }

            val price = currentItem.marketItem.price * count
            if(user.crystals < price) {
              logger.debug { "Player ${user.username} (${user.id}) tried to buy item: ${marketItem.id} ($price crystals), but does not has enough crystals (user: ${user.crystals} crystals, delta: ${user.crystals - price} crystals)" }
              return
            }
            user.crystals -= price

            socket.battlePlayer?.let { battlePlayer ->
              Command(CommandName.SetItemCount, marketItem.id, currentItem.count.toString()).send(battlePlayer)
            }

            logger.debug { "Bought supply ${marketItem.id} (count: $count, $price crystals)" }
          }
        }
      }

      else                      -> {
        logger.warn { "Buying item ${marketItem::class.simpleName} is not implemented" }
      }
    }

    if(isNewItem) {
      entityManager.persist(currentItem)
    }

    if(!isNewItem && currentItem is ServerGarageUserItemWithModification) {
      if(currentItem.modificationIndex < 3) {
        val oldModification = currentItem.modificationIndex
        currentItem.modificationIndex++

        withContext(Dispatchers.IO) {
          entityManager
            .createQuery("UPDATE ServerGarageUserItemWithModification SET modificationIndex = :modificationIndex WHERE id = :id")
            .setParameter("modificationIndex", currentItem.modificationIndex)
            .setParameter("id", currentItem.id)
            .executeUpdate()
        }

        val price = currentItem.modification.price
        if(user.crystals < price) {
          logger.debug { "Player ${user.username} (${user.id}) tried to buy item: ${marketItem.id} ($price crystals), but does not has enough crystals (user: ${user.crystals} crystals, delta: ${user.crystals - price} crystals)" }
          return
        }
        user.crystals -= price

        logger.debug { "Upgraded ${marketItem.id} modification: M${oldModification} -> M${currentItem.modificationIndex} ($price crystals)" }
      }
    }

    entityManager.transaction.commit()
    entityManager.close()

    userRepository.updateUser(user)

    Command(
      CommandName.BuyItem,
      BuyItemResponseData(
        itemId = marketItem.id,
        count = if(marketItem is ServerGarageItemSupply) count else 1
      ).toJson()
    ).send(socket)

    socket.updateCrystals()
  }

  /*
  SENT    : garage;kitBought;universal_soldier_m0
  SENT    : garage;try_mount_item;railgun_m0
  SENT    : garage;try_mount_item;twins_m0
  SENT    : garage;try_mount_item;flamethrower_m0
  SENT    : garage;try_mount_item;hornet_m0
  RECEIVED: LOBBY;add_crystall;179
  RECEIVED: GARAGE;showCategory;armor
  RECEIVED: GARAGE;select;hornet_m0
  RECEIVED: GARAGE;mount_item;railgun_m0;true
  RECEIVED: GARAGE;mount_item;twins_m0;true
  RECEIVED: GARAGE;mount_item;flamethrower_m0;true
  RECEIVED: GARAGE;mount_item;hornet_m0;true
  */
  @CommandHandler(CommandName.TryBuyKit)
  suspend fun tryBuyKit(socket: UserSocket, rawItem: String) {
    val user = socket.user ?: throw Exception("No User")

    val item = rawItem.substringBeforeLast("_")
    val marketItem = marketRegistry.get(item)
    if(marketItem !is ServerGarageItemKit) return

    logger.debug { "Trying to buy kit ${marketItem.id}..." }

    marketItem.kit.items.forEach { kitItem ->
      val kitMarketItem = marketRegistry.get(kitItem.id.substringBeforeLast("_"))
      TODO()
    }
  }
}
