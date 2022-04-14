package jp.assasans.protanki.server.commands.handlers

import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Clock
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import jp.assasans.protanki.server.HibernateUtils
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler
import jp.assasans.protanki.server.garage.*

class AuthHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  @CommandHandler(CommandName.Login)
  suspend fun login(socket: UserSocket, captcha: String, rememberMe: Boolean, username: String, password: String) {
    logger.debug { "User login: [ Username = '$username', Password = '$password', Captcha = ${if(captcha.isEmpty()) "*none*" else "'${captcha}'"}, Remember = $rememberMe ]" }

    val user = HibernateUtils.createEntityManager().let { entityManager ->
      // TODO(Assasans): Testing only
      var user = UserRepository().getUser(username)
      if(user == null) {
        entityManager.transaction.begin()

        user = User(
          id = 0,
          username = username,
          password = password,
          score = 1_000_000,
          crystals = 10_000_000,

          items = mutableListOf()
        )

        user.items += listOf(
          ServerGarageUserItemWeapon(user, "smoky", modificationIndex = 0),
          ServerGarageUserItemWeapon(user, "railgun", modificationIndex = 0),
          ServerGarageUserItemWeapon(user, "thunder", modificationIndex = 0),
          ServerGarageUserItemHull(user, "hunter", modificationIndex = 0),
          ServerGarageUserItemHull(user, "hornet", modificationIndex = 0),
          ServerGarageUserItemHull(user, "wasp", modificationIndex = 0),
          ServerGarageUserItemPaint(user, "green"),
          ServerGarageUserItemPaint(user, "zeus"),
          ServerGarageUserItemPaint(user, "moonwalker"),
          ServerGarageUserItemSupply(user, "health", count = 100),
          ServerGarageUserItemSupply(user, "armor", count = 100),
          ServerGarageUserItemSupply(user, "double_damage", count = 100),
          ServerGarageUserItemSupply(user, "n2o", count = 100),
          ServerGarageUserItemSubscription(user, "premium_effect", startTime = Clock.System.now(), duration = 10.days)
        )
        user.equipment = UserEquipment(
          hullId = "wasp",
          weaponId = "railgun",
          paintId = "moonwalker"
        )
        user.equipment.user = user

        entityManager.persist(user)
        user.items.forEach { item -> entityManager.persist(item) }

        entityManager.transaction.commit()
        entityManager.close()

        logger.debug { "Create user in database: ${user.username}" }
      }

      user
    }

    logger.debug { "Got user from database: ${user.username}" }

    // if(user.password == password) {
    logger.debug { "User login allowed" }

    socket.user = user
    Command(CommandName.AuthAccept).send(socket)
    socket.loadLobby()
    // } else {
    //   logger.debug { "User login rejected: incorrect password" }
    //
    //   Command(CommandName.AuthDenied).send(socket)
    // }
  }

  @CommandHandler(CommandName.LoginByHash)
  suspend fun loginByHash(socket: UserSocket, hash: String) {
    logger.debug { "User login by hash: $hash" }

    Command(CommandName.LoginByHashFailed).send(socket)
  }
}
