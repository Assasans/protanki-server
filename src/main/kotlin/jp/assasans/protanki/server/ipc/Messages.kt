package jp.assasans.protanki.server.ipc

import com.squareup.moshi.Json

abstract class ProcessMessage {
  override fun toString() = "${this::class.simpleName}"
}

// TODO(Assasans): Automatic Response messages

class ServerStartingMessage : ProcessMessage()
class ServerStartedMessage : ProcessMessage()

class ServerStopRequest : ProcessMessage()
class ServerStopResponse : ProcessMessage()
