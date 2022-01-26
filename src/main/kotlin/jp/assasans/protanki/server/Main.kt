package jp.assasans.protanki.server

import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
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
import org.koin.logger.SLF4JLogger
import jp.assasans.protanki.server.battles.BattleProcessor
import jp.assasans.protanki.server.client.UserSocket

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
      .bind(InetSocketAddress("127.0.0.1", 1337))

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

  val helloModule = module {
    single { SocketServer() }
    single { BattleProcessor() }
  }

  startKoin {
    logger(SLF4JLogger(Level.ERROR))

    modules(helloModule)
  }

  val server = Server()

  runBlocking { server.run() }
}
