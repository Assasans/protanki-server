package jp.assasans.protanki.server.commands.handlers

import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Clock
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import jp.assasans.protanki.server.BonusType
import jp.assasans.protanki.server.HibernateUtils
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler
import jp.assasans.protanki.server.garage.*
import jp.assasans.protanki.server.quests.*

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

          items = mutableListOf(),
          dailyQuests = mutableListOf()
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

        // TODO(Assasans): Testing only
        fun addQuest(index: Int, type: KClass<out ServerDailyQuest>, args: Map<String, Any?> = emptyMap()) {
          fun getParameter(name: String) = type.primaryConstructor!!.parameters.single { it.name == name }

          val quest = type.primaryConstructor!!.callBy(mapOf(
            getParameter("id") to 0,
            getParameter("user") to user,
            getParameter("questIndex") to index,
            getParameter("current") to 0,
            getParameter("required") to 2,
            getParameter("new") to true,
            getParameter("completed") to false,
            getParameter("rewards") to mutableListOf<ServerDailyQuestReward>()
          ) + args.mapKeys { (name) -> getParameter(name) })
          quest.rewards += listOf(
            ServerDailyQuestReward(quest, 0, type = ServerDailyRewardType.Crystals, count = 1_000_000),
            ServerDailyQuestReward(quest, 1, type = ServerDailyRewardType.Premium, count = 3)
          )

          entityManager.persist(quest)
          quest.rewards.forEach { reward -> entityManager.persist(reward) }

          user.dailyQuests.add(quest)
        }

        addQuest(0, JoinBattleMapQuest::class, mapOf("map" to "map_island"))
        addQuest(1, DeliverFlagQuest::class)
        addQuest(2, TakeBonusQuest::class, mapOf("bonus" to BonusType.Gold))

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
