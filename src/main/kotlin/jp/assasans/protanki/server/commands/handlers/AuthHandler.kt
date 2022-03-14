package jp.assasans.protanki.server.commands.handlers

import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Clock
import mu.KotlinLogging
import jp.assasans.protanki.server.client.User
import jp.assasans.protanki.server.client.UserEquipment
import jp.assasans.protanki.server.client.UserSocket
import jp.assasans.protanki.server.client.send
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler
import jp.assasans.protanki.server.garage.*

class AuthHandler : ICommandHandler {
  private val logger = KotlinLogging.logger { }

  @CommandHandler(CommandName.Login)
  suspend fun login(socket: UserSocket, captcha: String, rememberMe: Boolean, username: String, password: String) {
    logger.debug { "User login: [ Username = '$username', Password = '$password', Captcha = ${if(captcha.isEmpty()) "*none*" else "'${captcha}'"}, Remember = $rememberMe ]" }

    // val user = transaction {
    //   User.fromDatabase(Users
    //     .select { Users.username.lowerCase() eq data.login.lowercase() }
    //     .single()
    //   )
    // }

    val items = listOf(
      ServerGarageUserItemWeapon(socket.marketRegistry.get(GarageItemType.Weapon, "smoky"), modificationIndex = 0),
      ServerGarageUserItemWeapon(socket.marketRegistry.get(GarageItemType.Weapon, "railgun"), modificationIndex = 3),
      ServerGarageUserItemWeapon(socket.marketRegistry.get(GarageItemType.Weapon, "thunder"), modificationIndex = 3),
      ServerGarageUserItemHull(socket.marketRegistry.get(GarageItemType.Hull, "hunter"), modificationIndex = 0),
      ServerGarageUserItemHull(socket.marketRegistry.get(GarageItemType.Hull, "hornet"), modificationIndex = 3),
      ServerGarageUserItemHull(socket.marketRegistry.get(GarageItemType.Hull, "wasp"), modificationIndex = 0),
      ServerGarageUserItemPaint(socket.marketRegistry.get(GarageItemType.Paint, "green")),
      ServerGarageUserItemPaint(socket.marketRegistry.get(GarageItemType.Paint, "zeus")),
      ServerGarageUserItemSupply(socket.marketRegistry.get(GarageItemType.Supply, "health"), count = 100),
      ServerGarageUserItemSupply(socket.marketRegistry.get(GarageItemType.Supply, "armor"), count = 100),
      ServerGarageUserItemSupply(socket.marketRegistry.get(GarageItemType.Supply, "double_damage"), count = 100),
      ServerGarageUserItemSupply(socket.marketRegistry.get(GarageItemType.Supply, "n2o"), count = 100),
      ServerGarageUserItemSubscription(
        socket.marketRegistry.get(GarageItemType.Subscription, "premium_effect"),
        startTime = Clock.System.now(),
        duration = 10.days
      )
    )

    val user = User(
      id = 0,
      username = username,
      password = password,
      score = 123456,
      crystals = 123456,

      items = items,
      equipment = UserEquipment(
        hull = items.single { item -> item.marketItem.id == "wasp" } as ServerGarageUserItemHull,
        weapon = items.single { item -> item.marketItem.id == "railgun" } as ServerGarageUserItemWeapon,
        paint = items.single { item -> item.marketItem.id == "zeus" } as ServerGarageUserItemPaint
      )
    )

    // if(user.password == password) {
    logger.debug { "User login allowed" }

    socket.user = user
    socket.send(Command(CommandName.AuthAccept))
    socket.loadLobby()
    // } else {
    //   logger.debug { "User login rejected: incorrect password" }
    //
    //   socket.send(Command(CommandName.AuthDenied))
    // }
  }

  @CommandHandler(CommandName.LoginByHash)
  suspend fun loginByHash(socket: UserSocket, hash: String) {
    logger.debug { "User login by hash: $hash" }

    Command(CommandName.LoginByHashFailed).send(socket)
  }
}
