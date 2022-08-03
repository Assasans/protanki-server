package jp.assasans.protanki.server.ipc

import com.squareup.moshi.Moshi
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import jp.assasans.protanki.server.client.toJson

class WebSocketNetworking(val url: String) : IProcessNetworking, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val json: Moshi by inject()

  private val client = HttpClient(CIO) {
    install(WebSockets)
  }

  private var socket: ClientWebSocketSession? = null

  private val _events = MutableSharedFlow<ProcessMessage>()
  override val events = _events.asSharedFlow()

  override suspend fun run() {
    val socket = client.webSocketSession(url)
    this.socket = socket

    socket.launch {
      for(frame in socket.incoming) {
        if(frame !is Frame.Text) continue

        val content = frame.readText()
        try {
          val message = json.adapter(ProcessMessage::class.java).fromJson(content)
          if(message == null) {
            logger.warn { "[IPC] Invalid message: $content" }
            continue
          }

          // logger.debug { "[IPC] Received: $message" }
          _events.emit(message)
        } catch(exception: Exception) {
          logger.error(exception) { "[IPC] Error on message: $content" }
        }
      }
    }
  }

  override suspend fun send(message: ProcessMessage) {
    val socket = socket ?: throw IllegalStateException("Socket is not initialized")

    socket.send(message.toJson())
    logger.debug { "[IPC] Sent: $message" }
  }

  override suspend fun close() {
    socket?.close()
    client.close()

    logger.info { "Stopped IPC client" }
  }
}
