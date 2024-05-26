package jp.assasans.protanki.server.commands.handlers

import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler
import jp.assasans.protanki.server.invite.IInviteService

object AuthHandlerConstants {
  const val InviteRequired = "Invite code is required to log in"

  fun getInviteInvalidUsername(username: String) = "This invite can only be used with the username \"$username\""
}

class AuthHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val userRepository: IUserRepository by inject()
  private val userSubscriptionManager: IUserSubscriptionManager by inject()
  private val inviteService: IInviteService by inject()

  @CommandHandler(CommandName.Login)
  suspend fun login(socket: UserSocket, captcha: String, rememberMe: Boolean, username: String, password: String) {
    val invite = socket.invite
    if(inviteService.enabled) {
      // TODO(Assasans): AuthDenied shows unnecessary "Password incorrect" modal
      if(invite == null) {
        Command(CommandName.ShowAlert, AuthHandlerConstants.InviteRequired).send(socket)
        Command(CommandName.AuthDenied).send(socket)
        return
      }

      invite.username?.let { inviteUsername ->
        if(username == inviteUsername || username.startsWith("${inviteUsername}_")) return@let

        Command(CommandName.ShowAlert, AuthHandlerConstants.getInviteInvalidUsername(inviteUsername)).send(socket)
        Command(CommandName.AuthDenied).send(socket)
        return
      }
    }

    logger.debug { "User login: [ Invite = '${socket.invite?.code}', Username = '$username', Password = '$password', Captcha = ${if(captcha.isEmpty()) "*none*" else "'${captcha}'"}, Remember = $rememberMe ]" }

    val user = userRepository.getUser(username)
               ?: userRepository.createUser(username, password)
               ?: TODO("Race condition")
    logger.debug { "Got user from database: ${user.username}" }

    if(inviteService.enabled && invite != null) {
      invite.username = user.username
      invite.updateUsername()
    }

    // if(user.password == password) {
    logger.debug { "User login allowed" }

    userSubscriptionManager.add(user)
    socket.user = user
    Command(CommandName.AuthAccept).send(socket)
    socket.loadLobby()
    // } else {
    //   logger.debug { "User login rejected: incorrect password" }
    //
    //   Command(CommandName.AuthDenied).send(socket)
    // }
  }

  @CommandHandler(CommandName.LoginByHash)
  suspend fun loginByHash(socket: UserSocket, hash: String) {
    if(inviteService.enabled && socket.invite == null) {
      Command(CommandName.ShowAlert, AuthHandlerConstants.InviteRequired).send(socket)
      Command(CommandName.AuthDenied).send(socket)
      return

      // TODO(Assasans): Check username
    }

    logger.debug { "User login by hash: $hash" }

    Command(CommandName.LoginByHashFailed).send(socket)
  }

  @CommandHandler(CommandName.ActivateInvite)
  suspend fun activateInvite(socket: UserSocket, code: String) {
    logger.debug { "Fetching invite: $code" }

    val invite = inviteService.getInvite(code)
    if(invite != null) {
      Command(CommandName.InviteValid).send(socket)
    } else {
      Command(CommandName.InviteInvalid).send(socket)
    }

    socket.invite = invite
  }

  @CommandHandler(CommandName.CheckUsernameRegistration)
  suspend fun checkUsernameRegistration(socket: UserSocket, username: String) {
    if(userRepository.getUser(username) != null) {
      // TODO(Assasans): Use "nickname_exist"
      Command(CommandName.CheckUsernameRegistrationClient, "incorrect").send(socket)
      return
    }

    // Pass-through
    Command(CommandName.CheckUsernameRegistrationClient, "not_exist").send(socket)
  }

  @CommandHandler(CommandName.RegisterUser)
  suspend fun registerUser(socket: UserSocket, username: String, password: String, captcha: String) {
    val invite = socket.invite
    if(inviteService.enabled) {
      // TODO(Assasans): "Reigster" button is not disabled after error
      if(invite == null) {
        Command(CommandName.ShowAlert, AuthHandlerConstants.InviteRequired).send(socket)
        return
      }

      invite.username?.let { inviteUsername ->
        if(username == inviteUsername || username.startsWith("${inviteUsername}_")) return@let

        Command(CommandName.ShowAlert, AuthHandlerConstants.getInviteInvalidUsername(inviteUsername)).send(socket)
        return
      }
    }

    logger.debug { "Register user: [ Invite = '${socket.invite?.code}', Username = '$username', Password = '$password', Captcha = ${if(captcha.isEmpty()) "*none*" else "'${captcha}'"} ]" }

    val user = userRepository.createUser(username, password)
               ?: TODO("User exists")

    if(inviteService.enabled && invite != null) {
      invite.username = user.username
      invite.updateUsername()
    }

    userSubscriptionManager.add(user)
    socket.user = user
    Command(CommandName.AuthAccept).send(socket)
    socket.loadLobby()
  }
}
