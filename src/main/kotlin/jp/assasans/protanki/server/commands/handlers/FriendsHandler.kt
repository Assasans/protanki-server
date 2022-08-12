package jp.assasans.protanki.server.commands.handlers

import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler

class FriendsHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val userRepository: IUserRepository by inject()

  @CommandHandler(CommandName.ShowFriendsList)
  suspend fun showFriendsList(socket: UserSocket) {
    Command(CommandName.ClientShowFriendsList, ShowFriendsModalData().toJson()).send(socket)
  }

  @CommandHandler(CommandName.CheckFriendUsername)
  suspend fun checkFriendUsername(socket: UserSocket, username: String) {
    val selfUser = socket.user ?: throw Exception("No User")

    logger.debug { "Check friend username: $username" }
    if(username != selfUser.username && userRepository.getUser(username) != null) {
      Command(CommandName.FriendUsernameExists).send(socket)
    } else {
      Command(CommandName.FriendUsernameNotExists).send(socket)
    }
  }

  @CommandHandler(CommandName.SendFriendRequest)
  suspend fun sendFriendRequest(socket: UserSocket, username: String) {
    socket.sendChat("[SendFriendRequest] Not implemented yet")
  }

  @CommandHandler(CommandName.AcceptFriendRequest)
  suspend fun acceptFriendRequest(socket: UserSocket, username: String) {
    socket.sendChat("[AcceptFriendRequest] Not implemented yet")
  }

  @CommandHandler(CommandName.FriendReject)
  suspend fun friendReject(socket: UserSocket, username: String) {
    socket.sendChat("[FriendReject] Not implemented yet")
  }

  @CommandHandler(CommandName.FriendRejectAll)
  suspend fun friendRejectAll(socket: UserSocket, username: String) {
    socket.sendChat("[FriendRejectAll] Not implemented yet")
  }

  @CommandHandler(CommandName.FriendRemove)
  suspend fun removeFriend(socket: UserSocket, username: String) {
    socket.sendChat("[RemoveFriend] Not implemented yet")
  }

  @CommandHandler(CommandName.FriendRemoveNewAccepted)
  suspend fun friendRemoveNewAccepted(socket: UserSocket, username: String) {
    // TODO(Assasans): Save in DB?
    Command(CommandName.ClientFriendRemoveNewAccepted, username).send(socket)
  }

  @CommandHandler(CommandName.FriendRemoveNewIncoming)
  suspend fun friendRemoveNewIncoming(socket: UserSocket, username: String) {
    // TODO(Assasans): Save in DB?
    Command(CommandName.ClientFriendRemoveNewIncoming, username).send(socket)
  }
}
