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

class InviteToggleRequest(@Json val enabled: Boolean) : ProcessMessage()
class InviteToggleResponse : ProcessMessage()

class InviteAddRequest(@Json val code: String) : ProcessMessage()
class InviteAddResponse(@Json val success: Boolean) : ProcessMessage()

class InviteDeleteRequest(@Json val code: String) : ProcessMessage()
class InviteDeleteResponse(@Json val success: Boolean) : ProcessMessage()

class InviteListRequest : ProcessMessage()
class InviteListResponse(@Json val invites: List<Invite>) : ProcessMessage() {
  data class Invite(
    @Json val id: Int,
    @Json val code: String
  )
}
