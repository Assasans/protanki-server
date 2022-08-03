package jp.assasans.protanki.server.ipc

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NullNetworking : IProcessNetworking {
  override val events: SharedFlow<ProcessMessage> = MutableSharedFlow<ProcessMessage>().asSharedFlow()

  override suspend fun run() {}
  override suspend fun send(message: ProcessMessage) {}
  override suspend fun close() {}
}
