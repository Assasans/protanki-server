package jp.assasans.protanki.server

import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import kotlin.coroutines.coroutineContext
import kotlin.io.path.absolute
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.logger.SLF4JLogger
import jp.assasans.protanki.server.battles.BattleProcessor
import jp.assasans.protanki.server.battles.IBattleProcessor
import jp.assasans.protanki.server.battles.map.IMapRegistry
import jp.assasans.protanki.server.battles.map.MapRegistry
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.CommandRegistry
import jp.assasans.protanki.server.commands.ICommandRegistry
import jp.assasans.protanki.server.extensions.gitVersion
import jp.assasans.protanki.server.garage.GarageItemConverter
import jp.assasans.protanki.server.garage.GarageMarketRegistry
import jp.assasans.protanki.server.garage.IGarageItemConverter
import jp.assasans.protanki.server.garage.IGarageMarketRegistry
import jp.assasans.protanki.server.lobby.chat.ILobbyChatManager
import jp.assasans.protanki.server.lobby.chat.LobbyChatManager
import jp.assasans.protanki.server.serialization.*

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

  suspend fun run()
}

class SocketServer : ISocketServer {
  private val logger = KotlinLogging.logger { }

  override val players: MutableList<UserSocket> = mutableListOf()

  private lateinit var server: ServerSocket

  override suspend fun run() {
    server = aSocket(ActorSelectorManager(Dispatchers.IO))
      .tcp()
      .bind(InetSocketAddress("0.0.0.0", 1337))

    logger.info { "Started TCP server on ${server.localAddress}" }

    while(true) {
      val tcpSocket = server.accept()
      val socket = UserSocket(coroutineContext, tcpSocket)
      players.add(socket)

      println("Socket accepted: ${socket.remoteAddress}")

      GlobalScope.launch { socket.handle() }
    }
  }
}

suspend fun main(args: Array<String>) {
  val logger = KotlinLogging.logger { }

  logger.info { "Hello, 世界!" }
  logger.info { "Version: ${BuildConfig.gitVersion}" }
  logger.info { "Root path: ${Paths.get("").absolute()}" }

  val module = module {
    single<ISocketServer> { SocketServer() }
    single<ICommandRegistry> { CommandRegistry() }
    single<IBattleProcessor> { BattleProcessor() }
    single<IResourceManager> { ResourceManager() }
    single<IGarageItemConverter> { GarageItemConverter() }
    single<IResourceConverter> { ResourceConverter() }
    single<IGarageMarketRegistry> { GarageMarketRegistry() }
    single<IMapRegistry> { MapRegistry() }
    single<ILobbyChatManager> { LobbyChatManager() }
    single {
      Moshi.Builder()
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
        .add(KotlinJsonAdapterFactory())
        .add(GarageItemTypeAdapter())
        .add(ResourceTypeAdapter())
        .add(ServerMapThemeAdapter())
        .add(BattleTeamAdapter())
        .add(BattleModeAdapter())
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
