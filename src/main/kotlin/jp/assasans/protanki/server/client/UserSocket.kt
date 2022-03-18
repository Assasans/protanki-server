package jp.assasans.protanki.server.client

import java.io.IOException
import kotlin.io.path.readText
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.primaryConstructor
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.java.KoinJavaComponent
import jp.assasans.protanki.server.EncryptionTransformer
import jp.assasans.protanki.server.IResourceManager
import jp.assasans.protanki.server.PacketProcessor
import jp.assasans.protanki.server.battles.Battle
import jp.assasans.protanki.server.battles.BattlePlayer
import jp.assasans.protanki.server.battles.BattleTank
import jp.assasans.protanki.server.battles.IBattleProcessor
import jp.assasans.protanki.server.commands.*
import jp.assasans.protanki.server.exceptions.UnknownCommandCategoryException
import jp.assasans.protanki.server.exceptions.UnknownCommandException
import jp.assasans.protanki.server.garage.*
import jp.assasans.protanki.server.readAvailable

suspend fun Command.send(socket: UserSocket) = socket.send(this)
suspend fun Command.send(player: BattlePlayer) = player.socket.send(this)
suspend fun Command.send(tank: BattleTank) = tank.socket.send(this)

suspend fun UserSocket.sendChat(message: String) = Command(
  CommandName.SendChatMessageClient,
  listOf(
    ChatMessage(
      name = "",
      rang = 0,
      message = message,
      system = true,
      yellow = true
    ).toJson()
  )
).send(this)

@OptIn(ExperimentalStdlibApi::class)
class UserSocket(
  private val socket: Socket
) : KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val packetProcessor = PacketProcessor()
  private val encryption = EncryptionTransformer()
  private val commandRegistry by inject<ICommandRegistry>()
  private val resourceManager by inject<IResourceManager>()
  private val marketRegistry by inject<IGarageMarketRegistry>()
  private val garageItemConverter by inject<IGarageItemConverter>()
  private val battleProcessor by inject<IBattleProcessor>()
  private val json by inject<Moshi>()

  private val input: ByteReadChannel = socket.openReadChannel()
  private val output: ByteWriteChannel = socket.openWriteChannel(autoFlush = true)

  private val lock: Semaphore = Semaphore(1)
  // private val sendQueue: Queue<Command> = LinkedList()

  private val socketJobs: MutableList<Job> = mutableListOf()

  val remoteAddress: SocketAddress
    get() = socket.remoteAddress

  var active: Boolean = false

  var locale: SocketLocale? = null

  var user: User? = null
  var selectedBattle: Battle? = null
  var screen: Screen? = null

  val battle: Battle?
    get() = battlePlayer?.battle

  val battlePlayer: BattlePlayer?
    get() = battleProcessor.battles
      .flatMap { battle -> battle.players }
      .singleOrNull { player -> player.socket == this }

  private fun deactivate() {
    active = false

    val player = battlePlayer
    if(player != null) { // Remove player from battle
      // TODO(Assasans): Send leave command
      player.battle.players.remove(player)
    }

    logger.debug { "Cancelling ${socketJobs.size} jobs..." }
    socketJobs.forEach { job ->
      if(job.isActive) job.cancel()
    }
  }

  suspend fun send(command: Command) {
    lock.withPermit {
      try {
        output.writeFully(command.serialize().toByteArray())
      } catch(exception: IOException) {
        logger.warn(exception) { "${socket.remoteAddress} thrown an exception" }
        deactivate()
        return
      }

      if(
        command.name != CommandName.Pong &&
        command.name != CommandName.ClientMove &&
        command.name != CommandName.ClientFullMove &&
        command.name != CommandName.ClientRotateTurret &&
        command.name != CommandName.ClientMovementControl
      ) { // Too verbose
        if(
          command.name == CommandName.LoadResources ||
          command.name == CommandName.InitLocale ||
          command.name == CommandName.InitShotsData ||
          command.name == CommandName.InitGarageItems ||
          command.name == CommandName.InitGarageMarket
        ) { // Too long
          logger.trace { "Sent command ${command.name} ${command.args.drop(1)}" }
        } else {
          logger.trace { "Sent command ${command.name} ${command.args}" }
        }
      }
    }
  }

  private val dependenciesChannel: Channel<Int> = Channel(32) // TODO(Assasans)
  private val loadedDependencies: MutableList<Int> = mutableListOf()
  private var lastDependencyId = 1

  suspend fun loadDependency(resources: String): Int {
    Command(
      CommandName.LoadResources,
      listOf(
        resources,
        lastDependencyId.toString()
      )
    ).send(this)

    return lastDependencyId++
  }

  suspend fun markDependencyLoaded(id: Int) {
    dependenciesChannel.send(id)
  }

  suspend fun awaitDependency(id: Int) {
    if(loadedDependencies.contains(id)) return

    while(true) {
      val loaded = dependenciesChannel.receive()
      loadedDependencies.add(loaded)

      if(loaded == id) break
    }
  }

  private suspend fun processPacket(packet: String) {
    try {
      // val end = packet.takeLast(Command.Delimiter.length)
      // if(end != Command.Delimiter) throw Exception("Invalid packet end: $end")

      // val decrypted = encryption.decrypt(packet.dropLast(Command.Delimiter.length))
      if(packet.isEmpty()) return

      // logger.debug { "PKT: $packet" }
      val decrypted = encryption.decrypt(packet)

      // logger.debug { "Decrypt: $packet -> $decrypted" }

      val command = Command()
      command.readFrom(decrypted.toByteArray())

      if(
        command.name != CommandName.Ping &&
        command.name != CommandName.Move &&
        command.name != CommandName.FullMove &&
        command.name != CommandName.RotateTurret &&
        command.name != CommandName.MovementControl
      ) { // Too verbose
        logger.trace { "Received command ${command.name} ${command.args}" }
      }

      if(command.side != CommandSide.Server) throw Exception("Unsupported command: ${command.category}::${command.name}")

      val handler = commandRegistry.getHandler(command.name)
      if(handler != null) {
        try {
          val instance = handler.type.primaryConstructor!!.call()
          val args = mutableMapOf<KParameter, Any?>(
            Pair(
              handler.function.parameters.single { parameter -> parameter.kind == KParameter.Kind.INSTANCE },
              instance
            ),
            Pair(
              handler.function.parameters.filter { parameter -> parameter.kind == KParameter.Kind.VALUE }[0],
              this
            )
          )

          when(handler.argsBehaviour) {
            ArgsBehaviourType.Arguments -> {
              args.putAll(handler.args.mapIndexed { index, parameter ->
                val value = command.args[index]

                Pair(parameter, CommandArgs.convert(parameter.type, value))
              })
            }

            ArgsBehaviourType.Raw       -> {
              val argsParameter = handler.function.parameters.filter { parameter -> parameter.kind == KParameter.Kind.VALUE }[1]
              args[argsParameter] = CommandArgs(command.args)
            }
          }

          // logger.debug { "Handler ${handler.name} call arguments: ${args.map { argument -> "${argument.key.type}" }}" }

          handler.function.callSuspendBy(args)
        } catch(exception: Throwable) {
          logger.error(exception) { "Failed to call ${command.name} handler" }
        }

        return
      }

      when(command.name) {
        CommandName.ShowFriendsList -> {
          Command(
            CommandName.ShowFriendsList,
            listOf(ShowFriendsModalData().toJson())
          ).send(this)
        }

        else                        -> {}
      }
    } catch(exception: UnknownCommandCategoryException) {
      logger.warn { "Unknown command category: ${exception.category}" }
    } catch(exception: UnknownCommandException) {
      logger.warn { "Unknown command: ${exception.category}::${exception.command}" }
    } catch(exception: Exception) {
      logger.error(exception) { "An exception occurred" }
    }
  }

  suspend fun initBattleLoad() {
    Command(CommandName.StartLayoutSwitch, listOf("BATTLE")).send(this)
    Command(CommandName.UnloadBattleSelect).send(this)
    Command(CommandName.StartBattle).send(this)
    Command(CommandName.UnloadChat).send(this)
  }

  suspend fun <R> runConnected(block: suspend UserSocket.() -> R) {
    coroutineScope {
      val job = launch {
        block.invoke(this@UserSocket)
      }

      socketJobs.add(job)
      job.invokeOnCompletion { socketJobs.remove(job) }
    }
  }

  suspend fun handle() {
    active = true

    // awaitDependency can deadlock execution if suspended
    GlobalScope.launch { initClient() }

    try {
      while(!(input.isClosedForRead || input.isClosedForWrite)) {
        val buffer: ByteArray;
        try {
          buffer = input.readAvailable()
          packetProcessor.write(buffer)
        } catch(exception: IOException) {
          logger.warn(exception) { "${socket.remoteAddress} thrown an exception" }
          deactivate()

          break
        }

        // val packets = String(buffer).split(Command.Delimiter)

        // for(packet in packets) {
        // awaitDependency can deadlock execution if suspended
        //   GlobalScope.launch { processPacket(packet) }
        // }

        while(true) {
          val packet = packetProcessor.tryGetPacket() ?: break

          // awaitDependency can deadlock execution if suspended
          GlobalScope.launch { processPacket(packet) }
        }
      }

      logger.debug { "${socket.remoteAddress} end of data" }

      deactivate()
    } catch(exception: Throwable) {
      logger.error(exception) { "An exception occurred" }

      // withContext(Dispatchers.IO) {
      //   socket.close()
      // }
    }
  }

  suspend fun loadGarageResources() {
    awaitDependency(loadDependency(resourceManager.get("resources/garage.json").readText()))
  }

  suspend fun loadLobbyResources() {
    awaitDependency(loadDependency(resourceManager.get("resources/lobby.json").readText()))
  }

  suspend fun loadLobby() {
    Command(CommandName.StartLayoutSwitch, listOf("BATTLE_SELECT")).send(this)

    screen = Screen.BattleSelect

    Command(CommandName.InitPremium, listOf(InitPremiumData().toJson())).send(this)

    val user = user ?: throw Exception("No User")

    Command(
      CommandName.InitPanel,
      listOf(
        InitPanelData(
          name = user.username,
          crystall = user.crystals,
          rang = user.rank.value,
          score = user.score,
          currentRankScore = user.currentRankScore,
          next_score = user.nextRankScore
        ).toJson()
      )
    ).send(this)

    // Command(CommandName.UpdateRankProgress, listOf("3668")).send(this)

    Command(
      CommandName.InitFriendsList,
      listOf(
        InitFriendsListData(
          friends = listOf(
            // FriendEntry(id = "Luminate", rank = 16, online = false),
            // FriendEntry(id = "MoscowCity", rank = 18, online = true)
          )
        ).toJson()
      )
    ).send(this)

    loadLobbyResources()

    Command(CommandName.EndLayoutSwitch, listOf("BATTLE_SELECT", "BATTLE_SELECT")).send(this)

    Command(
      CommandName.ShowAchievements,
      listOf(ShowAchievementsData(ids = listOf(1, 3)).toJson())
    ).send(this)

    Command(
      CommandName.InitMessages,
      listOf(
        "{\"messages\":[{\"name\":\"1234raketa\",\"rang\":6,\"chatPermissions\":0,\"message\":\"+\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"RUNDAI1\",\"rang\":15,\"chatPermissions\":0,\"message\":\"есть такое)\",\"addressed\":true,\"chatPermissionsTo\":1,\"nameTo\":\"GVA\",\"rangTo\":15,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"1234raketa\",\"rang\":6,\"chatPermissions\":0,\"message\":\"странно привыкать к краскам с резитами)\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"RUNDAI1\",\"rang\":15,\"chatPermissions\":0,\"message\":\"я до уо1 чисто с релей играл)\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Red_Dragon\",\"rangTo\":7,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"S.Shadows-Brazil\",\"rang\":17,\"chatPermissions\":0,\"message\":\"#battle|Wasp and Fire|302969a56405e2b0 ro Wasp y Fire.\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"GVA\",\"rang\":15,\"chatPermissions\":1,\"message\":\"тут краски каченные еще до м4..\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"DougZ\",\"rang\":17,\"chatPermissions\":0,\"message\":\"#battle|Wasp and Fire|302969a56405e2b0 Entra ai wasp and fire\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"M_O_N_A_S_H_K_A\",\"rang\":7,\"chatPermissions\":0,\"message\":\"#battle|qartvelebo moit |ffcd251fcc756d6e\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"1234raketa\",\"rangTo\":6,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"1234raketa\",\"rang\":6,\"chatPermissions\":0,\"message\":\"#battle|qartvelebo moit |ffcd251fcc756d6e\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"https://www.youtube.com/watch?v=zzDf4tRUmfU\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"RUNDAI1\",\"rang\":15,\"chatPermissions\":0,\"message\":\"На канале zzeress проходит стрим! Заходи, будет интересно! https://youtu.be/QN2qwgn7YyA\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"koa noobs\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Waqtf\",\"rang\":8,\"chatPermissions\":0,\"message\":\"ктото хочет на полигон СР или на тишину СТF?\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"hahahahah boa mano\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Toguro\",\"rangTo\":14,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"Qual é o nome da música\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Toguro\",\"rangTo\":14,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"zhurv\",\"rang\":12,\"chatPermissions\":0,\"message\":\"#battle|Плато CTF|2979f072843c800a\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"musica do brasil\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"funk\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"eu entendo, mas queria o nome\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Toguro\",\"rangTo\":14,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"\",\"rang\":0,\"chatPermissions\":0,\"message\":\"Большинство угонов аккаунтов происходит из-за слишком простых паролей. Задумайтесь над этим, сделайте пароль сложнее, чаще его меняйте! Защитите свой аккаунт от злоумышленников\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"sourceUserIp\":\"null\",\"targetUserIp\":\"null\",\"system\":true,\"yellow\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"da primeira\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Toguro\",\"rangTo\":14,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"e uma mulher q canta ela\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"https://www.youtube.com/watch?v=zzDf4tRUmfU\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"ei\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"SOAD\",\"rang\":5,\"chatPermissions\":0,\"message\":\"#battle|Стрим WebSter и Zzeress|5480f4d421c4df0e\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"alguem sabe me dizer\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"S.Shadows-Brazil\",\"rang\":17,\"chatPermissions\":0,\"message\":\"#battle|Wasp y Fire brazil|8031c0ec28f272f7\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"yo\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Kratos\",\"rangTo\":5,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"se eu tivesse uma conta antigamente\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Waqtf\",\"rang\":8,\"chatPermissions\":0,\"message\":\"#battle|Тишина CTF|e85a3b80b4e6bae3\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"não, é um jogo independente do tanki\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Kratos\",\"rangTo\":5,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"eu posso logar nela nesse pro tanl?\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"RUNDAI1\",\"rang\":15,\"chatPermissions\":0,\"message\":\"На канале zzeress проходит стрим! Заходи, будет интересно! https://youtu.be/QN2qwgn7YyA\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"a tabom vlw\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"RUNDAI1\",\"rang\":15,\"chatPermissions\":0,\"message\":\"#battle|Стрим WebSter и Zzeress|5480f4d421c4df0e\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"RUNDAI1\",\"rang\":15,\"chatPermissions\":0,\"message\":\"#battle|Стрим WebSter и Zzeress|5480f4d421c4df0e\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"RUNDAI1\",\"rang\":15,\"chatPermissions\":0,\"message\":\"#battle|Стрим WebSter и Zzeress|5480f4d421c4df0e\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"alguma dica pra conseguir cristal\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"renatozikaa\",\"rang\":13,\"chatPermissions\":0,\"message\":\"é isso ai msm\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"o video tá fixe, deu sub\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Toguro\",\"rangTo\":14,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"mano voce fala muito bem em brasileiro parabens\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"dei*\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Toguro\",\"rangTo\":14,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"renatozikaa\",\"rang\":13,\"chatPermissions\":0,\"message\":\"vou sair tn\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"e upar?\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Toguro\",\"rang\":14,\"chatPermissions\":0,\"message\":\"ok ty\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"eu sou de Portugal, é fácil lol\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Toguro\",\"rangTo\":14,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Kratos\",\"rang\":5,\"chatPermissions\":0,\"message\":\"angel tu e youtuber?\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":true},{\"name\":\"S.Shadows-Brazil\",\"rang\":17,\"chatPermissions\":0,\"message\":\"0 partidas. F\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"bem me parecia\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"graças a deus um tuga\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"Compra cristais\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Kratos\",\"rangTo\":5,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"Também és tuga?\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"claro\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"maravilha\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"fds, deste o real grind nesta merda\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"A_Viuva_Preta_Br\",\"rang\":10,\"chatPermissions\":0,\"message\":\"#battle|wasp and fire|7d1ad7e732b33147\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"Comprei muito e jogo desde que o jogo saiu\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"A_Viuva_Preta_Br\",\"rang\":10,\"chatPermissions\":0,\"message\":\"#battle|wasp and fire|7d1ad7e732b33147\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"ah ok\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"A_Viuva_Preta_Br\",\"rang\":10,\"chatPermissions\":0,\"message\":\"#battle|wasp and fire|7d1ad7e732b33147\",\"addressed\":false,\"chatPermissionsTo\":0,\"nameTo\":\"\",\"rangTo\":0,\"system\":false,\"sourceUserPremium\":false,\"targetUserPremium\":false},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"Lisboeta?\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"do Porto ahahha\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"Portuense :)\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"Ah eu sou de Cascais\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"devo ir ao Porto no Verão\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"é bem mano, depois adiciona\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"Porto melhor cidade :v\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"hahahah estive no Porto em 2014\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Angel.Shark\",\"rang\":22,\"chatPermissions\":0,\"message\":\"é bonito mas prefiro Lisboa\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Richardo\",\"rangTo\":16,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true},{\"name\":\"Richardo\",\"rang\":16,\"chatPermissions\":0,\"message\":\"eu nunca fui a capital por acaso\",\"addressed\":true,\"chatPermissionsTo\":0,\"nameTo\":\"Angel.Shark\",\"rangTo\":22,\"system\":false,\"sourceUserPremium\":true,\"targetUserPremium\":true}]}",
        "{\"antiFloodEnabled\":true,\"typingSpeedAntifloodEnabled\":true,\"bufferSize\":60,\"minChar\":60,\"minWord\":5,\"showLinks\":true,\"admin\":false,\"selfName\":\"roflanebalo\",\"chatModeratorLevel\":0,\"symbolCost\":176,\"enterCost\":880,\"chatEnabled\":true,\"linksWhiteList\":[\"h\",\"t\",\"t\",\"p\",\":\",\"/\",\"/\",\"g\",\"t\",\"a\",\"n\",\"k\",\"s\",\"-\",\"o\",\"n\",\"l\",\"i\",\"n\",\"e\",\".\",\"c\",\"o\",\"m\",\"/\",\"|\",\"h\",\"t\",\"t\",\"p\",\":\",\"/\",\"/\",\"v\",\"k\",\".\",\"c\",\"o\",\"m\",\"/\",\"e\",\"b\",\"a\",\"l\"]}"
        // json.adapter(InitChatMessagesData::class.java).toJson(
        //   InitChatMessagesData(
        //     messages = listOf(
        //       ChatMessage(name = "roflanebalo", rang = 4, message = "Ты пидорас")
        //     )
        //   )
        // ),
        // json.adapter(InitChatSettings::class.java).toJson(InitChatSettings())
      )
    ).send(this)

    initBattleList()
  }

  private suspend fun initClient() {
    Command(CommandName.InitExternalModel, listOf("http://localhost/")).send(this)
    Command(
      CommandName.InitRegistrationModel,
      listOf(
        // "{\"bgResource\": 122842, \"enableRequiredEmail\": false, \"maxPasswordLength\": 100, \"minPasswordLength\": 1}"
        InitRegistrationModelData(
          enableRequiredEmail = false
        ).toJson()
      )
    ).send(this)

    Command(CommandName.InitLocale, listOf(resourceManager.get("lang/ru.json").readText())).send(this)

    awaitDependency(loadDependency(resourceManager.get("resources/auth.json").readText()))
    Command(CommandName.MainResourcesLoaded).send(this)
  }

  suspend fun initBattleList() {
    val mapsParsed = json
      .adapter<List<Map>>(Types.newParameterizedType(List::class.java, Map::class.java))
      .fromJson(resourceManager.get("maps.json").readText())!!

    Command(
      CommandName.InitBattleCreate,
      listOf(
        InitBattleCreateData(
          battleLimits = listOf(
            BattleLimit(battleMode = "DM", scoreLimit = 999, timeLimitInSec = 59940),
            BattleLimit(battleMode = "TDM", scoreLimit = 999, timeLimitInSec = 59940),
            BattleLimit(battleMode = "CTF", scoreLimit = 999, timeLimitInSec = 59940),
            BattleLimit(battleMode = "CP", scoreLimit = 999, timeLimitInSec = 59940)
          ),
          maps = mapsParsed
        ).toJson()
      )
    ).send(this)

    Command(
      CommandName.InitBattleSelect,
      listOf(
        InitBattleSelectData(
          battles = listOf(
            BattleData(
              battleId = "493202bf695cc88a",
              battleMode = "DM",
              map = "map_kungur",
              name = "ProTanki Server",
              maxPeople = 8,
              minRank = 0,
              maxRank = 30,
              preview = 476411,
              users = listOf(
                "Luminate"
              )
            )
          )
        ).toJson()
      )
    ).send(this)
  }

  suspend fun initGarage() {
    val user = user ?: throw Exception("No User")

    val itemsParsed = mutableListOf<GarageItem>()
    val marketParsed = mutableListOf<GarageItem>()

    val marketItems = marketRegistry.items

    marketItems.forEach { (_, marketItem) ->
      val userItem = user.items.singleOrNull { it.marketItem == marketItem }
      val clientMarketItems = when(marketItem) {
        is ServerGarageItemWeapon       -> garageItemConverter.toClientWeapon(marketItem)
        is ServerGarageItemHull         -> garageItemConverter.toClientHull(marketItem)
        is ServerGarageItemPaint        -> listOf(garageItemConverter.toClientPaint(marketItem))
        is ServerGarageItemSupply       -> listOf(garageItemConverter.toClientSupply(marketItem))
        is ServerGarageItemSubscription -> listOf(garageItemConverter.toClientSubscription(marketItem))
        is ServerGarageItemKit          -> listOf(garageItemConverter.toClientKit(marketItem))
        is ServerGarageItemPresent      -> listOf(garageItemConverter.toClientPresent(marketItem))

        else                            -> throw NotImplementedError("Not implemented: ${marketItem::class.simpleName}")
      }

      // if(marketItem is ServerGarageItemSupply) return@forEach
      // if(marketItem is ServerGarageItemSubscription) return@forEach
      // if(marketItem is ServerGarageItemKit) return@forEach

      if(userItem != null) {
        // Add user item
        if(userItem is ServerGarageUserItemSupply) {
          clientMarketItems.single().count = userItem.count
        }

        if(userItem is IServerGarageUserItemWithModification) {
          clientMarketItems.forEach clientMarketItems@{ clientItem ->
            // Add current and previous modifications as user items
            // if(clientItem.modificationID!! <= userItem.modification) itemsParsed.add(clientItem)

            // if(clientItem.modificationID!! < userItem.modification) return@clientMarketItems
            if(clientItem.modificationID == userItem.modificationIndex) itemsParsed.add(clientItem)
            else marketParsed.add(clientItem)
          }
        } else {
          itemsParsed.addAll(clientMarketItems)
        }
      } else {
        // Add market item
        marketParsed.addAll(clientMarketItems)
      }
    }

    marketParsed
      .filter { item -> item.type == GarageItemType.Kit }
      .forEach { item ->
        if(item.kit == null) throw Exception("Kit is null")

        val ownsAll = item.kit.kitItems.all { kitItem ->
          val id = kitItem.id.substringBeforeLast("_")
          val modification = kitItem.id
            .substringAfterLast("_")
            .drop(1) // Drop 'm' letter
            .toInt()

          marketParsed.none { marketItem -> marketItem.id == id && marketItem.modificationID == modification }
        }
        if(ownsAll) {
          marketParsed.remove(item)

          logger.debug { "Removed kit ${item.name} from market: user owns all items" }
        }
      }

    Command(CommandName.InitGarageItems, listOf(InitGarageItemsData(items = itemsParsed).toJson())).send(this)
    Command(
      CommandName.InitMountedItem,
      listOf(user.equipment.hull.mountName, user.equipment.hull.modification.object3ds.toString())
    ).send(this)
    Command(
      CommandName.InitMountedItem,
      listOf(user.equipment.weapon.mountName, user.equipment.weapon.modification.object3ds.toString())
    ).send(this)
    Command(
      CommandName.InitMountedItem,
      listOf(user.equipment.paint.mountName, user.equipment.paint.marketItem.coloring.toString())
    ).send(this)
    Command(CommandName.InitGarageMarket, listOf(InitGarageMarketData(items = marketParsed).toJson())).send(this)

    // logger.debug { "User items:" }
    // itemsParsed
    //   .filter { item -> item.type != GarageItemType.Paint }
    //   .forEach { item -> logger.debug { "  > ${item.name} (m${item.modificationID})" } }
    //
    // logger.debug { "Market items:" }
    // marketParsed
    //   .filter { item -> item.type != GarageItemType.Paint }
    //   .forEach { item -> logger.debug { "  > ${item.name} (m${item.modificationID})" } }
  }

  suspend fun updateCrystals() {
    val user = user ?: throw Exception("User data is not loaded")

    Command(CommandName.SetCrystals, listOf(user.crystals.toString())).send(this)
  }
}

data class InitBonusesData(
  @Json val init_bonuses: List<Any> = listOf() // TODO(Assasans)
)

inline fun <reified T : Any> T.toJson(json: Moshi): String {
  return json.adapter(T::class.java).toJson(this)
}

inline fun <reified T : Any> T.toJson(): String {
  val json = KoinJavaComponent.inject<Moshi>(Moshi::class.java).value
  return json.adapter(T::class.java).toJson(this)
}

fun <T : Any> Moshi.toJson(value: T): String {
  return adapter<T>(value::class.java).toJson(value)
}

data class InitBattleModelData(
  @Json val battleId: String,
  @Json val map_id: String,
  @Json val mapId: Int,
  @Json val kick_period_ms: Int = 125000,
  @Json val invisible_time: Int = 3500,
  @Json val spectator: Boolean = true,
  @Json val reArmorEnabled: Boolean,
  @Json val active: Boolean = true,
  @Json val dustParticle: Int = 110001,
  @Json val minRank: Int = 3,
  @Json val maxRank: Int = 30,
  @Json val skybox: String,
  @Json val sound_id: Int = 584396,
  @Json val map_graphic_data: String
)

data class BonusLightingData(
  @Json val attenuationBegin: Int = 100,
  @Json val attenuationEnd: Int = 500,
  @Json val color: Int,
  @Json val intensity: Int = 1,
  @Json val time: Int = 0
)

data class BonusData(
  @Json val lighting: BonusLightingData,
  @Json val id: String,
  @Json val resourceId: Int,
  @Json val lifeTime: Int = 30
)

data class InitBonusesDataData(
  @Json val bonuses: List<BonusData>,
  @Json val cordResource: Int = 1000065,
  @Json val parachuteInnerResource: Int = 170005,
  @Json val parachuteResource: Int = 170004,
  @Json val pickupSoundResource: Int = 269321
)

data class ShowFriendsModalData(
  @Json val new_incoming_friends: List<FriendEntry> = listOf(),
  @Json val new_accepted_friends: List<FriendEntry> = listOf()
)

data class BattleUser(
  @Json val user: String,
  @Json val kills: Int = 0,
  @Json val score: Int = 0,
  @Json val suspicious: Boolean = false
)

abstract class ShowBattleInfoData(
  @Json val itemId: String,
  @Json val battleMode: String,
  @Json val scoreLimit: Int,
  @Json val timeLimitInSec: Int,
  @Json val preview: Int,
  @Json val maxPeopleCount: Int,
  @Json val name: String,
  @Json val proBattle: Boolean = false,
  @Json val minRank: Int,
  @Json val maxRank: Int,
  @Json val roundStarted: Boolean = true,
  @Json val spectator: Boolean,
  @Json val withoutBonuses: Boolean,
  @Json val withoutCrystals: Boolean,
  @Json val withoutSupplies: Boolean,
  @Json val proBattleEnterPrice: Int = 150,
  @Json val timeLeftInSec: Int,
  @Json val userPaidNoSuppliesBattle: Boolean = false,
  @Json val proBattleTimeLeftInSec: Int = -1
)

class ShowTeamBattleInfoData(
  itemId: String,
  battleMode: String,
  scoreLimit: Int,
  timeLimitInSec: Int,
  preview: Int,
  maxPeopleCount: Int,
  name: String,
  proBattle: Boolean = false,
  minRank: Int,
  maxRank: Int,
  roundStarted: Boolean = true,
  spectator: Boolean,
  withoutBonuses: Boolean,
  withoutCrystals: Boolean,
  withoutSupplies: Boolean,
  proBattleEnterPrice: Int = 150,
  timeLeftInSec: Int,
  userPaidNoSuppliesBattle: Boolean = false,
  proBattleTimeLeftInSec: Int = -1,

  @Json val usersRed: List<BattleUser>,
  @Json val usersBlue: List<BattleUser>,

  @Json val scoreRed: Int = 0,
  @Json val scoreBlue: Int = 0,

  @Json val autoBalance: Boolean,
  @Json val friendlyFire: Boolean,
) : ShowBattleInfoData(
  itemId,
  battleMode,
  scoreLimit,
  timeLimitInSec,
  preview,
  maxPeopleCount,
  name,
  proBattle,
  minRank,
  maxRank,
  roundStarted,
  spectator,
  withoutBonuses,
  withoutCrystals,
  withoutSupplies,
  proBattleEnterPrice,
  timeLeftInSec,
  userPaidNoSuppliesBattle,
  proBattleTimeLeftInSec
)

class ShowDmBattleInfoData(
  itemId: String,
  battleMode: String,
  scoreLimit: Int,
  timeLimitInSec: Int,
  preview: Int,
  maxPeopleCount: Int,
  name: String,
  proBattle: Boolean = false,
  minRank: Int,
  maxRank: Int,
  roundStarted: Boolean = true,
  spectator: Boolean,
  withoutBonuses: Boolean,
  withoutCrystals: Boolean,
  withoutSupplies: Boolean,
  proBattleEnterPrice: Int = 150,
  timeLeftInSec: Int,
  userPaidNoSuppliesBattle: Boolean = false,
  proBattleTimeLeftInSec: Int = -1,

  @Json val users: List<BattleUser>,
  @Json val score: Int = 0,
) : ShowBattleInfoData(
  itemId,
  battleMode,
  scoreLimit,
  timeLimitInSec,
  preview,
  maxPeopleCount,
  name,
  proBattle,
  minRank,
  maxRank,
  roundStarted,
  spectator,
  withoutBonuses,
  withoutCrystals,
  withoutSupplies,
  proBattleEnterPrice,
  timeLeftInSec,
  userPaidNoSuppliesBattle,
  proBattleTimeLeftInSec
)

data class BattleData(
  @Json val battleId: String,
  @Json val battleMode: String,
  @Json val map: String,
  @Json val maxPeople: Int,
  @Json val name: String,
  @Json val privateBattle: Boolean = false,
  @Json val proBattle: Boolean = false,
  @Json val minRank: Int,
  @Json val maxRank: Int,
  @Json val preview: Int,
  @Json val suspicious: Boolean = false,
  @Json val users: List<String>
)

data class InitBattleSelectData(
  @Json val battles: List<BattleData>
)

data class BattleLimit(
  @Json val battleMode: String,
  @Json val scoreLimit: Int,
  @Json val timeLimitInSec: Int,
)

data class Map(
  @Json val enabled: Boolean = true,
  @Json val mapId: String,
  @Json val mapName: String,
  @Json val maxPeople: Int,
  @Json val preview: Int,
  @Json val maxRank: Int,
  @Json val minRank: Int,
  @Json val supportedModes: List<String>,
  @Json val theme: String
)

data class InitBattleCreateData(
  @Json val maxRangeLength: Int = 7,
  @Json val battleCreationDisabled: Boolean = false,
  @Json val battleLimits: List<BattleLimit>,
  @Json val maps: List<Map>
)

data class ShowAchievementsData(
  @Json val ids: List<Int>
)

data class ChatMessage(
  @Json val name: String,
  @Json val rang: Int,
  @Json val chatPermissions: Int = 0,
  @Json val message: String,
  @Json val addressed: Boolean = false,
  @Json val chatPermissionsTo: Int = 0,
  @Json val nameTo: String = "",
  @Json val rangTo: Int = 0,
  @Json val system: Boolean = false,
  @Json val yellow: Boolean = false,
  @Json val sourceUserPremium: Boolean = false,
  @Json val targetUserPremium: Boolean = false
)

data class InitChatMessagesData(
  @Json val messages: List<ChatMessage>
)

data class InitChatSettings(
  @Json val antiFloodEnabled: Boolean = true,
  @Json val typingSpeedAntifloodEnabled: Boolean = true,
  @Json val bufferSize: Int = 60,
  @Json val minChar: Int = 60,
  @Json val minWord: Int = 5,
  @Json val showLinks: Boolean = true,
  @Json val admin: Boolean = false,
  @Json val selfName: String,
  @Json val chatModeratorLevel: Int = 0,
  @Json val symbolCost: Int = 176,
  @Json val enterCost: Int = 880,
  @Json val chatEnabled: Boolean = true,
  @Json val linksWhiteList: List<String> = "http://gtanks-online.com/|http://vk.com/ebal"
    .toCharArray()
    .map(Char::toString)
)

data class AuthData(
  @Json val captcha: String,
  @Json val remember: Boolean,
  @Json val login: String,
  @Json val password: String
)

data class InitRegistrationModelData(
  @Json val bgResource: Int = 122842,
  @Json val enableRequiredEmail: Boolean = false,
  @Json val maxPasswordLength: Int = 100,
  @Json val minPasswordLength: Int = 1
)

data class InitPremiumData(
  @Json val left_time: Int = -1,
  @Json val needShowNotificationCompletionPremium: Boolean = false,
  @Json val needShowWelcomeAlert: Boolean = false,
  @Json val reminderCompletionPremiumTime: Int = 86400,
  @Json val wasShowAlertForFirstPurchasePremium: Boolean = false,
  @Json val wasShowReminderCompletionPremium: Boolean = true
)

data class InitPanelData(
  @Json val name: String,
  @Json val crystall: Int,
  @Json val email: String? = null,
  @Json val tester: Boolean = false,
  @Json val next_score: Int,
  @Json val place: Int = 0,
  @Json val rang: Int,
  @Json val rating: Int = 1,
  @Json val score: Int,
  @Json val currentRankScore: Int,
  @Json val hasDoubleCrystal: Boolean = false,
  @Json val durationCrystalAbonement: Int = -1,
  @Json val userProfileUrl: String = "http://ratings.generaltanks.com/ru/user/"
)

data class FriendEntry(
  @Json val id: String,
  @Json val rank: Int,
  @Json val online: Boolean
)

data class InitFriendsListData(
  @Json val friends: List<FriendEntry> = listOf(),
  @Json val incoming: List<FriendEntry> = listOf(),
  @Json val outcoming: List<FriendEntry> = listOf(),
  @Json val new_incoming_friends: List<FriendEntry> = listOf(),
  @Json val new_accepted_friends: List<FriendEntry> = listOf()
)

data class ShowSettingsData(
  @Json val emailNotice: Boolean = false,
  @Json val email: String? = null,
  @Json val notificationEnabled: Boolean = true,
  @Json val showDamageEnabled: Boolean = true,
  @Json val isConfirmEmail: Boolean = false,
  @Json val authorizationUrl: String = "http://localhost/",
  @Json val linkExists: Boolean = false,
  @Json val snId: String = "vkontakte",
  @Json val passwordCreated: Boolean = true
)
