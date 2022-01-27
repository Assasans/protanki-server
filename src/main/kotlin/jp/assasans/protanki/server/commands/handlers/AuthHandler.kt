package jp.assasans.protanki.server.commands.handlers

import mu.KotlinLogging
import org.jetbrains.exposed.sql.ResultRow
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

    var id: Int = 0
    lateinit var username: String
    lateinit var password: String
    var score: Int = 0
    var crystals: Int = 0

    transaction {
      val user = Users
        .select { Users.username.lowerCase() eq data.login.lowercase() }
        .single()

      id = user[Users.id].value
      username = user[Users.username]
      password = user[Users.password]
      score = user[Users.score]
      crystals = user[Users.crystals]
    }

    if(password != data.password) {
      logger.debug { "User login rejected: incorrect password" }

      socket.send(Command(CommandName.AuthDenied))
    } else {
      logger.debug { "User login allowed" }

      socket.user = User(
        id = id,
        username = username,
        rank = UserRank.Major,
        score = score,
        crystals = crystals
      )
      socket.send(Command(CommandName.AuthAccept))
      socket.loadLobby()
    }
  }
}
