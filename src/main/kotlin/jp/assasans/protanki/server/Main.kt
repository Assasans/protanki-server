package jp.assasans.protanki.server

import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent
import org.koin.logger.SLF4JLogger
import jp.assasans.protanki.server.battles.BattleProcessor
import jp.assasans.protanki.server.battles.IBattleProcessor
import jp.assasans.protanki.server.client.InitBonusesData
import jp.assasans.protanki.server.client.UserSocket
import jp.assasans.protanki.server.client.toJson
import jp.assasans.protanki.server.commands.CommandRegistry
import jp.assasans.protanki.server.commands.ICommandRegistry

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
  suspend fun run()
}

class SocketServer : ISocketServer {
  private val logger = KotlinLogging.logger { }

  private lateinit var server: ServerSocket

  override suspend fun run() {
    server = aSocket(ActorSelectorManager(Dispatchers.IO))
      .tcp()
      .bind(InetSocketAddress("0.0.0.0", 1337))

    logger.info { "Started TCP server on ${server.localAddress}" }

    while(true) {
      val tcpSocket = server.accept()
      val socket = UserSocket(tcpSocket)

      println("Socket accepted: ${socket.remoteAddress}")

      GlobalScope.launch { socket.handle() }
    }
  }
}

fun main(args: Array<String>) {
  val logger = KotlinLogging.logger { }

  logger.info { "Hello, 世界!" }
  logger.info { "Root path: ${Paths.get("").absolute()}" }

  val module = module {
    single { SocketServer() as ISocketServer }
    single { CommandRegistry() as ICommandRegistry }
    single { BattleProcessor() as IBattleProcessor }
    single { Database() as IDatabase }
    single {
      Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    }
  }

  startKoin {
    logger(SLF4JLogger(Level.ERROR))

    modules(module)
  }

  val server = Server()


  runBlocking { server.run() }
}
