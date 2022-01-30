package jp.assasans.protanki.server.commands.handlers

import mu.KotlinLogging
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import jp.assasans.protanki.server.client.AuthData
import jp.assasans.protanki.server.client.User
import jp.assasans.protanki.server.client.UserSocket
import jp.assasans.protanki.server.client.send
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

    // val user = transaction {
    //   User.fromDatabase(Users
    //     .select { Users.username.lowerCase() eq data.login.lowercase() }
    //     .single()
    //   )
    // }

    val user = User(
      id = 0,
      username = data.login,
      password = data.password,
      score = 123456,
      crystals = 123456
    )

    // if(user.password == data.password) {
    logger.debug { "User login allowed" }

    socket.user = user
    socket.send(Command(CommandName.AuthAccept))
    socket.loadLobby()
    // } else {
    //   logger.debug { "User login rejected: incorrect password" }
    //
    //   socket.send(Command(CommandName.AuthDenied))
    // }
  }

  @CommandHandler(CommandName.LoginByHash)
  suspend fun loginByHash(socket: UserSocket, hash: String) {
    logger.debug { "User login by hash: $hash" }

    Command(CommandName.LoginByHashFailed).send(socket)
  }
}
