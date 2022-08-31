package jp.assasans.protanki.server

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.reflect.KClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.logger.SLF4JLogger
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import kotlinx.coroutines.CancellationException
import jp.assasans.protanki.server.api.IApiServer
import jp.assasans.protanki.server.api.WebApiServer
import jp.assasans.protanki.server.battles.BattleProcessor
import jp.assasans.protanki.server.battles.DamageCalculator
import jp.assasans.protanki.server.battles.IBattleProcessor
import jp.assasans.protanki.server.battles.IDamageCalculator
import jp.assasans.protanki.server.battles.map.IMapRegistry
import jp.assasans.protanki.server.battles.map.MapRegistry
import jp.assasans.protanki.server.chat.ChatCommandRegistry
import jp.assasans.protanki.server.chat.IChatCommandRegistry
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.CommandRegistry
import jp.assasans.protanki.server.commands.ICommandRegistry
import jp.assasans.protanki.server.extensions.cast
import jp.assasans.protanki.server.extensions.gitVersion
import jp.assasans.protanki.server.garage.GarageItemConverter
import jp.assasans.protanki.server.garage.GarageMarketRegistry
import jp.assasans.protanki.server.garage.IGarageItemConverter
import jp.assasans.protanki.server.garage.IGarageMarketRegistry
import jp.assasans.protanki.server.invite.IInviteRepository
import jp.assasans.protanki.server.invite.IInviteService
import jp.assasans.protanki.server.invite.InviteRepository
import jp.assasans.protanki.server.invite.InviteService
import jp.assasans.protanki.server.ipc.IProcessNetworking
import jp.assasans.protanki.server.ipc.NullNetworking
import jp.assasans.protanki.server.ipc.ProcessMessage
import jp.assasans.protanki.server.ipc.WebSocketNetworking
import jp.assasans.protanki.server.lobby.chat.ILobbyChatManager
import jp.assasans.protanki.server.lobby.chat.LobbyChatManager
import jp.assasans.protanki.server.quests.IQuestConverter
import jp.assasans.protanki.server.quests.QuestConverter
import jp.assasans.protanki.server.resources.IResourceServer
import jp.assasans.protanki.server.resources.ResourceServer
import jp.assasans.protanki.server.serialization.*
import jp.assasans.protanki.server.store.IStoreItemConverter
import jp.assasans.protanki.server.store.IStoreRegistry
import jp.assasans.protanki.server.store.StoreItemConverter
import jp.assasans.protanki.server.store.StoreRegistry

suspend fun ByteReadChannel.readAvailable(): ByteArray {
  val data = ByteArrayOutputStream()
  val temp = ByteArray(1024)
  // while(!isClosedForRead) {
  val read = readAvailable(temp)
  if(read > 0) {
    data.write(temp, 0, read)
  }
  // }

  return data.toByteArray()
}

interface ISocketServer {
  val players: MutableList<UserSocket>

  suspend fun run(scope: CoroutineScope)
  suspend fun stop()
}

class SocketServer : ISocketServer {
  private val logger = KotlinLogging.logger { }

  override val players: MutableList<UserSocket> = mutableListOf()

  private lateinit var server: ServerSocket

  private var acceptJob: Job? = null

  override suspend fun run(scope: CoroutineScope) {
    server = aSocket(ActorSelectorManager(Dispatchers.IO))
      .tcp()
      .bind(InetSocketAddress("0.0.0.0", 1337))

    logger.info { "Started TCP server on ${server.localAddress}" }

    acceptJob = scope.launch {
      try {
        val coroutineScope = CoroutineScope(scope.coroutineContext + SupervisorJob())

        while(true) {
          val tcpSocket = server.accept()
          val socket = UserSocket(coroutineContext, tcpSocket)
          players.add(socket)

          println("Socket accepted: ${socket.remoteAddress}")

          coroutineScope.launch { socket.handle() }
        }
      } catch(exception: CancellationException) {
        logger.debug { "Client accept job cancelled" }
      } catch(exception: Exception) {
        logger.error(exception) { "Exception in client accept loop" }
      }
    }
  }

  override suspend fun stop() {
    // TODO(Assasans): Hack to prevent ConcurrentModificationException
    players.toList().forEach { player -> player.deactivate() }
    acceptJob?.cancel()
    withContext(Dispatchers.IO) { server.close() }

    logger.info { "Stopped game server" }
  }
}

fun main(args: Array<String>) = object : CliktCommand() {
  val ipcUrl by option("--ipc-url", help = "IPC server URL")

  override fun run() = runBlocking {
    val logger = KotlinLogging.logger { }

    logger.info { "Hello, 世界!" }
    logger.info { "Version: ${BuildConfig.gitVersion}" }
    logger.info { "Root path: ${Paths.get("").absolute()}" }

    val module = module {
      single<IProcessNetworking> {
        when(val url = ipcUrl) {
          null -> NullNetworking()
          else -> WebSocketNetworking(url)
        }
      }
      single<ISocketServer> { SocketServer() }
      single<IResourceServer> { ResourceServer() }
      single<IApiServer> { WebApiServer() }
      single<ICommandRegistry> { CommandRegistry() }
      single<IBattleProcessor> { BattleProcessor() }
      single<IResourceManager> { ResourceManager() }
      single<IGarageItemConverter> { GarageItemConverter() }
      single<IResourceConverter> { ResourceConverter() }
      single<IGarageMarketRegistry> { GarageMarketRegistry() }
      single<IMapRegistry> { MapRegistry() }
      single<IStoreRegistry> { StoreRegistry() }
      single<IStoreItemConverter> { StoreItemConverter() }
      single<ILobbyChatManager> { LobbyChatManager() }
      single<IChatCommandRegistry> { ChatCommandRegistry() }
      single<IDamageCalculator> { DamageCalculator() }
      single<IQuestConverter> { QuestConverter() }
      single<IUserRepository> { UserRepository() }
      single<IUserSubscriptionManager> { UserSubscriptionManager() }
      single<IInviteService> { InviteService(enabled = false) }
      single<IInviteRepository> { InviteRepository() }
      single {
        Moshi.Builder()
          .add(
            PolymorphicJsonAdapterFactory.of(ProcessMessage::class.java, "_").let {
              var factory = it
              val reflections = Reflections("jp.assasans.protanki.server")

              reflections.get(Scanners.SubTypes.of(ProcessMessage::class.java).asClass<ProcessMessage>()).forEach { type ->
                val messageType = type.kotlin.cast<KClass<ProcessMessage>>()
                val name = messageType.simpleName ?: throw IllegalStateException("$messageType has no simple name")

                factory = factory.withSubtype(messageType.java, name.removeSuffix("Message"))
                logger.debug { "Registered IPC message: $name" }
              }

              factory
            }
          )
          .add(
            PolymorphicJsonAdapterFactory.of(WeaponVisual::class.java, "\$type")
              .withSubtype(SmokyVisual::class.java, "smoky")
              .withSubtype(RailgunVisual::class.java, "railgun")
              .withSubtype(ThunderVisual::class.java, "thunder")
              .withSubtype(FlamethrowerVisual::class.java, "flamethrower")
              .withSubtype(FreezeVisual::class.java, "freeze")
              .withSubtype(IsidaVisual::class.java, "isida")
              .withSubtype(TwinsVisual::class.java, "twins")
              .withSubtype(ShaftVisual::class.java, "shaft")
              .withSubtype(RicochetVisual::class.java, "ricochet")
          )
          .add(BattleDataJsonAdapterFactory())
          .add(LocalizedStringAdapterFactory())
          .add(ClientLocalizedStringAdapterFactory())
          .add(KotlinJsonAdapterFactory())
          .add(GarageItemTypeAdapter())
          .add(ResourceTypeAdapter())
          .add(ServerMapThemeAdapter())
          .add(BattleTeamAdapter())
          .add(BattleModeAdapter())
          .add(IsidaFireModeAdapter())
          .add(BonusTypeMapAdapter())
          .add(SkyboxSideAdapter())
          .add(EquipmentConstraintsModeAdapter())
          .add(ChatModeratorLevelAdapter())
          .add(SocketLocaleAdapter())
          .add(StoreCurrencyAdapter())
          .add(ScreenAdapter())
          .add(SerializeNull.JSON_ADAPTER_FACTORY)
          .build()
      }
    }

    startKoin {
      logger(SLF4JLogger(Level.ERROR))

      modules(module)
    }

    val server = Server()

    server.run()
  }
}.main(args)
