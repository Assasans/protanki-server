package jp.assasans.protanki.server.commands.handlers

import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Clock
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import jp.assasans.protanki.server.BonusType
import jp.assasans.protanki.server.HibernateUtils
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler
import jp.assasans.protanki.server.garage.*
import jp.assasans.protanki.server.invite.IInviteService
import jp.assasans.protanki.server.quests.*

object AuthHandlerConstants {
  const val InviteRequired = "Invite code is required to log in"
}

class AuthHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val userRepository: IUserRepository by inject()
  private val userSubscriptionManager: IUserSubscriptionManager by inject()
  private val inviteService: IInviteService by inject()

  @CommandHandler(CommandName.Login)
  suspend fun login(socket: UserSocket, captcha: String, rememberMe: Boolean, username: String, password: String) {
    if(inviteService.enabled && socket.invite == null) {
      Command(CommandName.ShowAlert, AuthHandlerConstants.InviteRequired).send(socket)
      return
    }

    logger.debug { "User login: [ Invite = '${socket.invite?.code}', Username = '$username', Password = '$password', Captcha = ${if(captcha.isEmpty()) "*none*" else "'${captcha}'"}, Remember = $rememberMe ]" }

    val user = userRepository.getUser(username)
               ?: userRepository.createUser(username, password)
               ?: TODO("Race condition")
    logger.debug { "Got user from database: ${user.username}" }

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
      return
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
}
