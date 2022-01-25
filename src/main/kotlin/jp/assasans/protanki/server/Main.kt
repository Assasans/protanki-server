package jp.assasans.protanki.server

import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.logger.SLF4JLogger
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

fun main(args: Array<String>) {
  val logger = KotlinLogging.logger { }

  logger.info { "Hello, 世界!" }

  startKoin {
    logger(SLF4JLogger())

    // modules(helloModule)
  }

  runBlocking {
    val server = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress("127.0.0.1", 1337))
    println("Started echo telnet server at ${server.localAddress}")

    while(true) {
      val tcpSocket = server.accept()
      val socket = UserSocket(tcpSocket)

      println("Socket accepted: ${socket.remoteAddress}")

      launch { socket.handle() }
    }
  }
}
