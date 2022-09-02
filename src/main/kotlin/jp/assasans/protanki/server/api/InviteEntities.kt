package jp.assasans.protanki.server.api

import com.squareup.moshi.Json
import jp.assasans.protanki.server.invite.Invite

class EmptyResponse

data class ErrorResponse(
  @Json val error: Boolean = true,
  @Json val message: String
)

data class ToggleInviteServiceRequest(@Json val enabled: Boolean)
data class CreateInviteRequest(@Json val code: String)
data class DeleteInviteRequest(@Json val code: String)

data class GetInvitesResponse(
  @Json val enabled: Boolean,
  @Json val invites: List<InviteResponse>
)

data class InviteResponse(
  @Json val id: Int,
  @Json val code: String
)

fun Invite.toResponse(): InviteResponse = InviteResponse(
  id = id,
  code = code
)
