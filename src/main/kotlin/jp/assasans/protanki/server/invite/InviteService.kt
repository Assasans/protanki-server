package jp.assasans.protanki.server.invite

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IInviteService {
  var enabled: Boolean

  suspend fun getInvite(code: String): Invite?
}

class InviteService(
  override var enabled: Boolean
) : IInviteService, KoinComponent {
  private val inviteRepository: IInviteRepository by inject()

  override suspend fun getInvite(code: String): Invite? {
    return inviteRepository.getInvite(code)
  }
}
