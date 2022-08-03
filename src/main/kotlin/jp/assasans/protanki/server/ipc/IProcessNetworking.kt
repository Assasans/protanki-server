package jp.assasans.protanki.server.ipc

import org.koin.java.KoinJavaComponent
import kotlinx.coroutines.flow.SharedFlow

interface IProcessNetworking {
  val events: SharedFlow<ProcessMessage>

  suspend fun run()
  suspend fun send(message: ProcessMessage)
  suspend fun close()
}

suspend fun ProcessMessage.send(networking: IProcessNetworking) {
  networking.send(this)
}

suspend fun ProcessMessage.send() {
  val networking by KoinJavaComponent.getKoin().inject<IProcessNetworking>()
  send(networking)
}
