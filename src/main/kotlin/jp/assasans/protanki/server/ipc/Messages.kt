package jp.assasans.protanki.server.ipc

abstract class ProcessMessage {
  override fun toString() = "${this::class.simpleName}"
}

class ServerStartingMessage : ProcessMessage()
class ServerStartedMessage : ProcessMessage()

class ServerStopRequest : ProcessMessage()
class ServerStopResponse : ProcessMessage()
