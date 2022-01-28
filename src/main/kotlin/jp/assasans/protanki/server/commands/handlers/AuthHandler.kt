package jp.assasans.protanki.server.commands.handlers

import mu.KotlinLogging
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import jp.assasans.protanki.server.client.*
import jp.assasans.protanki.server.commands.Command
import jp.assasans.protanki.server.commands.CommandHandler
import jp.assasans.protanki.server.commands.CommandName
import jp.assasans.protanki.server.commands.ICommandHandler
import jp.assasans.protanki.server.entities.Users

class AuthHandler : ICommandHandler {
  private val logger = KotlinLogging.logger { }

  @CommandHandler(CommandName.Auth)
  suspend fun auth(socket: UserSocket, data: AuthData) {
    logger.debug { "User login: [ Username = '${data.login}', Password = '${data.password}', Captcha = ${if(data.captcha.isEmpty()) "*none*" else "'${data.captcha}'"} ]" }

    val user = transaction {
      User.fromDatabase(Users
        .select { Users.username.lowerCase() eq data.login.lowercase() }
        .single()
      )
    }

    if(false && user.password != data.password) {
      logger.debug { "User login rejected: incorrect password" }

      socket.send(Command(CommandName.AuthDenied))
    } else {
      logger.debug { "User login allowed" }

      socket.user = user
      socket.send(Command(CommandName.AuthAccept))
      socket.loadLobby()
    }
  }
}
