package jp.assasans.protanki.server.commands.handlers

import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Clock
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.IDatabase
import jp.assasans.protanki.server.client.User
import jp.assasans.protanki.server.client.UserEquipment
import jp.assasans.protanki.server.client.UserSocket
import jp.assasans.protanki.server.client.send
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler
import jp.assasans.protanki.server.garage.*

class AuthHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val database by inject<IDatabase>()
  private val marketRegistry by inject<IGarageMarketRegistry>()

  @CommandHandler(CommandName.Login)
  suspend fun login(socket: UserSocket, captcha: String, rememberMe: Boolean, username: String, password: String) {
    logger.debug { "User login: [ Username = '$username', Password = '$password', Captcha = ${if(captcha.isEmpty()) "*none*" else "'${captcha}'"}, Remember = $rememberMe ]" }

    var user = database.users.singleOrNull { user -> user.username == username }

    // TODO(Assasans): Testing only
    if(user == null) {
      val items = listOf(
        ServerGarageUserItemWeapon(marketRegistry.get("smoky").cast(), modificationIndex = 0),
        ServerGarageUserItemWeapon(marketRegistry.get("railgun").cast(), modificationIndex = 3),
        ServerGarageUserItemWeapon(marketRegistry.get("thunder").cast(), modificationIndex = 3),
        ServerGarageUserItemHull(marketRegistry.get("hunter").cast(), modificationIndex = 0),
        ServerGarageUserItemHull(marketRegistry.get("hornet").cast(), modificationIndex = 3),
        ServerGarageUserItemHull(marketRegistry.get("wasp").cast(), modificationIndex = 0),
        ServerGarageUserItemPaint(marketRegistry.get("green").cast()),
        ServerGarageUserItemPaint(marketRegistry.get("zeus").cast()),
        ServerGarageUserItemSupply(marketRegistry.get("health").cast(), count = 100),
        ServerGarageUserItemSupply(marketRegistry.get("armor").cast(), count = 100),
        ServerGarageUserItemSupply(marketRegistry.get("double_damage").cast(), count = 100),
        ServerGarageUserItemSupply(marketRegistry.get("n2o").cast(), count = 100),
        ServerGarageUserItemSubscription(marketRegistry.get("premium_effect").cast(), startTime = Clock.System.now(), duration = 10.days)
      )

      user = User(
        id = database.users.size + 1,
        username = username,
        password = password,
        score = 123456,
        crystals = 7654321,

        items = items,
        equipment = UserEquipment(
          hull = items.single { item -> item.marketItem.id == "wasp" } as ServerGarageUserItemHull,
          weapon = items.single { item -> item.marketItem.id == "railgun" } as ServerGarageUserItemWeapon,
          paint = items.single { item -> item.marketItem.id == "zeus" } as ServerGarageUserItemPaint
        )
      )
      database.users.add(user)

      logger.debug { "Create user in database: ${user.username}" }
    }

    logger.debug { "Got user from database: ${user.username}" }

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
