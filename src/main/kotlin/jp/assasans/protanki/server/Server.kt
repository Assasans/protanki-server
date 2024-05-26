package jp.assasans.protanki.server

import kotlin.coroutines.coroutineContext
import kotlin.random.Random
import kotlin.reflect.KClass
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import jp.assasans.protanki.server.api.IApiServer
import jp.assasans.protanki.server.battles.Battle
import jp.assasans.protanki.server.battles.BattleProperty
import jp.assasans.protanki.server.battles.BattleTeam
import jp.assasans.protanki.server.battles.IBattleProcessor
import jp.assasans.protanki.server.battles.bonus.*
import jp.assasans.protanki.server.battles.map.IMapRegistry
import jp.assasans.protanki.server.battles.map.get
import jp.assasans.protanki.server.battles.mode.CaptureTheFlagModeHandler
import jp.assasans.protanki.server.battles.mode.DeathmatchModeHandler
import jp.assasans.protanki.server.battles.mode.TeamDeathmatchModeHandler
import jp.assasans.protanki.server.chat.*
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler
import jp.assasans.protanki.server.commands.ICommandRegistry
import jp.assasans.protanki.server.extensions.cast
import jp.assasans.protanki.server.garage.*
import jp.assasans.protanki.server.invite.IInviteRepository
import jp.assasans.protanki.server.invite.IInviteService
import jp.assasans.protanki.server.ipc.*
import jp.assasans.protanki.server.math.Quaternion
import jp.assasans.protanki.server.math.nextVector3
import jp.assasans.protanki.server.resources.IResourceServer
import jp.assasans.protanki.server.store.IStoreRegistry

class Server : KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val processNetworking by inject<IProcessNetworking>()
  private val socketServer by inject<ISocketServer>()
  private val resourceServer by inject<IResourceServer>()
  private val apiServer by inject<IApiServer>()
  private val commandRegistry by inject<ICommandRegistry>()
  private val battleProcessor by inject<IBattleProcessor>()
  private val marketRegistry by inject<IGarageMarketRegistry>()
  private val mapRegistry by inject<IMapRegistry>()
  private val chatCommandRegistry by inject<IChatCommandRegistry>()
  private val storeRegistry by inject<IStoreRegistry>()
  private val userRepository by inject<IUserRepository>()
  private val inviteService by inject<IInviteService>()
  private val inviteRepository by inject<IInviteRepository>()

  private var networkingEventsJob: Job? = null

  suspend fun run() {
    logger.info { "Starting server..." }

    processNetworking.run()
    ServerStartingMessage().send()

    coroutineScope {
      launch { mapRegistry.load() }
      launch { marketRegistry.load() }
      launch { storeRegistry.load() }
    }

    val reflections = Reflections("jp.assasans.protanki.server")

    reflections.get(Scanners.SubTypes.of(ICommandHandler::class.java).asClass<ICommandHandler>()).forEach { type ->
      val handlerType = type.kotlin.cast<KClass<ICommandHandler>>()

      commandRegistry.registerHandlers(handlerType)
      logger.debug { "Registered command handler: ${handlerType.simpleName}" }
    }

    battleProcessor.battles.add(
      Battle(
        coroutineContext,
        id = "493202bf695cc88a",
        title = "ProTanki Server",
        map = mapRegistry.get("map_kungur", ServerMapTheme.SummerDay),
        modeHandlerBuilder = DeathmatchModeHandler.builder()
      )
    )

    battleProcessor.battles.add(
      Battle(
        coroutineContext,
        id = "493202bf695cc88b",
        title = "Damage test",
        map = mapRegistry.get("map_island", ServerMapTheme.SummerDay),
        modeHandlerBuilder = TeamDeathmatchModeHandler.builder()
      )
    )

    battleProcessor.battles.add(
      Battle(
        coroutineContext,
        id = "493202bf695cc88c",
        title = "Rank limited",
        map = mapRegistry.get("map_highland", ServerMapTheme.SummerDay),
        modeHandlerBuilder = CaptureTheFlagModeHandler.builder()
      ).also { battle ->
        battle.properties[BattleProperty.MinRank] = 1
        battle.properties[BattleProperty.MaxRank] = 3
      }
    )

    chatCommandRegistry.apply {
      command("help") {
        description("Show list of commands or help for a specific command")

        argument("command", String::class) {
          description("Command to show help for")
          optional()
        }

        handler {
          val commandName: String? = arguments.getOrNull("command")
          if(commandName == null) {
            reply("Available commands: ${commands.joinToString(", ") { command -> command.name }}")
            return@handler
          }

          val command = commands.singleOrNull { command -> command.name == commandName }
          if(command == null) {
            reply("Unknown command: $commandName")
            return@handler
          }

          val builder: StringBuilder = StringBuilder()

          builder.append(command.name)
          if(command.description != null) {
            builder.append(" - ${command.description}")
          }
          builder.append("\n")

          if(command.arguments.isNotEmpty()) {
            builder.appendLine("Arguments:")
            command.arguments.forEach { argument ->
              builder.append("    ")
              builder.append("${argument.name}: ${argument.type.simpleName}")
              if(argument.isOptional) builder.append(" (optional)")
              if(argument.description != null) {
                builder.append(" - ${argument.description}")
              }
              builder.appendLine()
            }
          }

          reply(builder.toString())
        }
      }

      command("kick") {
        description("Kick a user from the server")

        argument("user", String::class) {
          description("The user to kick")
        }

        handler {
          val username: String = arguments["user"]
          val player = socketServer.players.singleOrNull { socket -> socket.user?.username == username }
          if(player == null) {
            reply("User '$username' not found")
            return@handler
          }

          player.deactivate()
          if(player != socket) {
            reply("User '$username' has been kicked")
          }
        }
      }

      command("dump") {
        subcommand("battle") {
          handler {
            val battle = socket.battle
            if(battle == null) {
              reply("You are not in a battle")
              return@handler
            }

            val builder = StringBuilder()

            builder.appendLine("Battle:")
            builder.appendLine("    ID: ${battle.id}")
            builder.appendLine("    Name: ${battle.title}")
            builder.appendLine("Map:")
            builder.appendLine("    ID: ${battle.map.id}")
            builder.appendLine("    Name: ${battle.map.name}")
            builder.appendLine("    Theme: ${battle.map.theme.name}")
            builder.appendLine("Players:")
            battle.players.forEach { player ->
              builder.append("    - ${player.user.username}")

              val properties = mutableListOf<String>()

              properties.add("sequence: ${player.sequence}")

              if(player.team != BattleTeam.None) {
                properties.add("team: ${player.team.name}")
              }
              properties.add("score: ${player.score}")
              properties.add("kills: ${player.kills}")
              properties.add("deaths: ${player.deaths}")

              if(properties.isNotEmpty()) {
                builder.append(" (${properties.joinToString(", ")})")
              }
              builder.append("\n")

              val tank = player.tank
              if(tank != null) {
                builder.appendLine("        Tank: ${tank.id}/${tank.incarnation} (${tank.state})")
                builder.appendLine("            Position: ${tank.position}")
                builder.appendLine("            Orientation: ${tank.orientation}")
              }
            }
            builder.appendLine("Handler:")
            builder.appendLine("    Class: ${battle.modeHandler::class.simpleName}")
            builder.appendLine("    Mode: ${battle.modeHandler.mode.name}")
            battle.modeHandler.dump(builder)

            reply(builder.toString())
          }
        }
      }

      command("restart") {
        description("Finish and restart the current battle")

        handler {
          val battle = socket.battle
          if(battle == null) {
            reply("You are not in a battle")
            return@handler
          }

          battle.restart()
          reply("Battle finished")
        }
      }

      command("property") {
        subcommand("list") {
          handler {
            val battle = socket.battle
            if(battle == null) {
              reply("You are not in a battle")
              return@handler
            }

            val builder = StringBuilder()
            BattleProperty.values().forEach { property ->
              val value = battle.properties[property]

              builder.append("${property.key}: $value")
              if(property.defaultValue != null) {
                builder.append(" (default: ${property.defaultValue})")
              }
              builder.append("\n")
            }

            reply(builder.toString())
          }
        }

        subcommand("get") {
          argument("property", String::class) {
            description("The battle property key to get")
          }

          handler {
            val key: String = arguments["property"]

            val battle = socket.battle
            if(battle == null) {
              reply("You are not in a battle")
              return@handler
            }

            val builder = StringBuilder()

            val property = BattleProperty.getOrNull(key)
            if(property == null) {
              reply("No such property: $key")
              return@handler
            }

            val value = battle.properties[property]
            builder.append("${property.key}: $value")
            if(property.defaultValue != null) {
              builder.append(" (default: ${property.defaultValue})")
            }

            reply(builder.toString())
          }
        }

        subcommand("set") {
          argument("property", String::class) {
            description("The battle property key to set")
          }

          argument("value", String::class) {
            description("The value to set the property to")
          }

          handler {
            val key: String = arguments["property"]
            val value: String = arguments["value"]

            val battle = socket.battle
            if(battle == null) {
              reply("You are not in a battle")
              return@handler
            }

            val builder = StringBuilder()

            val property = BattleProperty.getOrNull(key)
            if(property == null) {
              reply("No such property: $key")
              return@handler
            }

            val oldValue = battle.properties[property]

            val typedValue: Any = when(property.type) {
              String::class  -> value
              Int::class     -> value.toInt()
              Double::class  -> value.toDouble()
              Boolean::class -> when {
                value.equals("false", ignoreCase = true) -> false
                value.equals("true", ignoreCase = true)  -> true
                else                                     -> throw Exception("Invalid Boolean value: $value")
              }

              else           -> throw Exception("Unsupported property type: ${property.type.qualifiedName}")
            }

            battle.properties.setValue(property, typedValue)
            builder.append("Changed $key: $oldValue -> $typedValue")

            reply(builder.toString())
          }
        }
      }

      command("bonus") {
        description("Manage battle bonuses")

        subcommand("spawn") {
          description("Spawn a bonus at random point")

          argument("type", String::class) {
            description("The type of bonus to spawn")
          }

          handler {
            val type: String = arguments["type"]

            val battle = socket.battle
            if(battle == null) {
              reply("You are not in a battle")
              return@handler
            }

            val bonusType = BonusType.get(type)
            if(bonusType == null) {
              reply("No such bonus: $type")
              return@handler
            }

            val bonusPoint = battle.map.bonuses
              .filter { bonus -> bonus.types.contains(bonusType) }
              .filter { bonus -> bonus.modes.contains(battle.modeHandler.mode) }
              .random()

            val position = Random.nextVector3(bonusPoint.position.min.toVector(), bonusPoint.position.max.toVector())
            val rotation = Quaternion()
            rotation.fromEulerAngles(bonusPoint.rotation.toVector())

            val bonus = when(bonusType) {
              BonusType.Health       -> BattleRepairKitBonus(battle, battle.bonusProcessor.nextId, position, rotation)
              BonusType.DoubleArmor  -> BattleDoubleArmorBonus(battle, battle.bonusProcessor.nextId, position, rotation)
              BonusType.DoubleDamage -> BattleDoubleDamageBonus(battle, battle.bonusProcessor.nextId, position, rotation)
              BonusType.Nitro        -> BattleNitroBonus(battle, battle.bonusProcessor.nextId, position, rotation)
              BonusType.Gold         -> BattleGoldBonus(battle, battle.bonusProcessor.nextId, position, rotation)
              else                   -> throw Exception("Unsupported bonus type: $bonusType")
            }

            battle.bonusProcessor.incrementId()
            battle.coroutineScope.launch {
              battle.bonusProcessor.spawn(bonus)
              reply("Spawned $bonusType at $position, $rotation (with parachute: ${bonusPoint.parachute})")
            }
          }
        }
      }

      command("addxp") {
        description("Add experience points to a user")
        alias("addscore")

        argument("amount", Int::class) {
          description("The amount of experience points to add")
        }

        argument("user", String::class) {
          description("The user to add experience points")
          optional()
        }

        handler {
          val amount: Int = arguments.get<String>("amount").toInt() // TODO(Assasans)
          val username: String? = arguments.getOrNull("user")

          val player = if(username != null) socketServer.players.find { it.user?.username == username } else socket
          if(player == null) {
            reply("Player not found: $username")
            return@handler
          }

          val user = player.user ?: throw Exception("User is null")

          user.score = (user.score + amount).coerceAtLeast(0)
          player.updateScore()
          userRepository.updateUser(user)

          reply("Added $amount experience points to ${user.username}")
        }
      }

      command("addcry") {
        description("Add crystals to a user")

        argument("amount", Int::class) {
          description("The amount of crystals to add")
        }

        argument("user", String::class) {
          description("The user to add crystals")
          optional()
        }

        handler {
          val amount: Int = arguments.get<String>("amount").toInt() // TODO(Assasans)
          val username: String? = arguments.getOrNull("user")

          val player = if(username != null) socketServer.players.find { it.user?.username == username } else socket
          if(player == null) {
            reply("Player not found: $username")
            return@handler
          }

          val user = player.user ?: throw Exception("User is null")

          user.crystals = (user.crystals + amount).coerceAtLeast(0)
          player.updateCrystals()
          userRepository.updateUser(user)

          reply("Added $amount crystals to ${user.username}")
        }
      }

      command("stop") {
        handler {
          reply("Stopping server...")
          jp.assasans.protanki.server.commands.Command(CommandName.ShowServerStop).let { command ->
            socketServer.players.forEach { player -> player.send(command) }
          }

          // TODO(Assasans)
          GlobalScope.launch { stop() }
        }
      }

      command("reset-items") {
        description("Reset all garage items")

        argument("user", String::class) {
          description("The user to reset items")
          optional()
        }

        handler {
          val username = arguments.getOrNull<String>("user")
          val user: User? = if(username != null) {
            userRepository.getUser(username)
          } else {
            socket.user ?: throw Exception("User is null")
          }
          if(user == null) {
            reply("User '$username' not found")
            return@handler
          }

          HibernateUtils.createEntityManager().let { entityManager ->
            entityManager.transaction.begin()

            user.items.clear()
            user.items += listOf(
              ServerGarageUserItemWeapon(user, "smoky", modificationIndex = 0),
              ServerGarageUserItemHull(user, "hunter", modificationIndex = 0),
              ServerGarageUserItemPaint(user, "green")
            )
            user.equipment.hullId = "hunter"
            user.equipment.weaponId = "smoky"
            user.equipment.paintId = "green"

            withContext(Dispatchers.IO) {
              entityManager
                .createQuery("DELETE FROM ServerGarageUserItem WHERE id.user = :user")
                .setParameter("user", user)
                .executeUpdate()
            }

            withContext(Dispatchers.IO) {
              entityManager
                .createQuery("UPDATE User SET equipment = :equipment WHERE id = :id")
                .setParameter("equipment", user.equipment)
                .setParameter("id", user.id)
                .executeUpdate()
            }

            user.items.forEach { item -> entityManager.persist(item) }

            entityManager.transaction.commit()
            entityManager.close()
          }

          socketServer.players.singleOrNull { player -> player.user?.id == user.id }?.let { target ->
            if(target.screen == Screen.Garage) {
              // Refresh garage to update items
              Command(CommandName.UnloadGarage).send(target)

              target.loadGarageResources()
              target.initGarage()
            }
          }

          reply("Reset garage items for user '${user.username}'")
        }
      }

      command("addfund") {
        description("Add fund to battle")

        argument("amount", Int::class) {
          description("The amount of fund crystals to add")
        }

        argument("battle", String::class) {
          description("The battle ID to add crystals")
          optional()
        }

        handler {
          val amount: Int = arguments.get<String>("amount").toInt() // TODO(Assasans)
          val battleId: String? = arguments.getOrNull("battle")

          val battle = if(battleId != null) battleProcessor.battles.singleOrNull { it.id == battleId } else socket.battle
          if(battle == null) {
            if(battleId != null) reply("Battle '$battleId' not found")
            else reply("You are not in a battle")

            return@handler
          }

          battle.fundProcessor.fund = (battle.fundProcessor.fund + amount).coerceAtLeast(0)
          battle.fundProcessor.updateFund()

          reply("Added $amount fund crystals to battle ${battle.id}")
        }
      }

      command("invite") {
        description("Manage invites")

        subcommand("toggle") {
          description("Toggle invite-only mode")

          argument("enabled", Boolean::class) {
            description("Invite-only mode enabled")
          }

          handler {
            val enabled = arguments.get<String>("enabled").toBooleanStrict()

            inviteService.enabled = enabled

            reply("Invite codes are now ${if(enabled) "required" else "not required"} to log in")
          }
        }

        subcommand("add") {
          description("Add new invite")

          argument("code", String::class) {
            description("Invite code to add")
          }

          handler {
            val code = arguments.get<String>("code")

            val invite = inviteRepository.createInvite(code)
            if(invite == null) {
              reply("Invite '$code' already exists")
              return@handler
            }

            reply("Added invite '${invite.code}' (ID: ${invite.id})")
          }
        }

        subcommand("delete") {
          description("Delete invite")

          argument("code", String::class) {
            description("Invite code to delete")
          }

          handler {
            val code = arguments.get<String>("code")

            if(!inviteRepository.deleteInvite(code)) {
              reply("Invite '$code' does not exist")
            }

            reply("Deleted invite '$code'")
          }
        }

        subcommand("list") {
          description("List existing invites")

          handler {
            val invites = inviteRepository.getInvites()
            if(invites.isEmpty()) {
              reply("No invites are available")
              return@handler
            }

            reply("Invites:\n${invites.joinToString("\n") { invite -> "  - ${invite.code} (ID: ${invite.id})" }}")
          }
        }
      }
    }

    // Initialize database
    HibernateUtils.createEntityManager().close()

    inviteRepository.createInvite("2112")

    coroutineScope {
      launch { resourceServer.run() }
      launch { apiServer.run() }

      socketServer.run(this)

      networkingEventsJob = processNetworking.events.onEach { event ->
        // logger.debug { "[IPC] Received event: $event" }
        when(event) {
          // TODO(Assasans)
          is ServerStopRequest -> {
            logger.info { "[IPC] Stopping server..." }
            GlobalScope.launch { stop() } // TODO(Assasans)
          }

          else                 -> logger.info { "[IPC] Unknown event: ${event::class.simpleName}" }
        }
      }.launchIn(this)

      ServerStartedMessage().send()
      logger.info { "Server started" }
    }
  }

  suspend fun stop() {
    networkingEventsJob?.cancel()
    coroutineScope {
      launch { socketServer.stop() }
      launch { resourceServer.stop() }
      launch { apiServer.stop() }
      launch { HibernateUtils.close() }
    }

    // Prevent exit before sending IPC message
    coroutineScope {
      launch {
        ServerStopResponse().send()
        processNetworking.close()
      }
    }
  }
}
