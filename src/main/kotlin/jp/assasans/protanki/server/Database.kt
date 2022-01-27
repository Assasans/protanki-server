package jp.assasans.protanki.server

import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import jp.assasans.protanki.server.client.User
import jp.assasans.protanki.server.entities.Users

interface IDatabase {
  val db: Database

  fun connect()
}

class Database : IDatabase {
  private val logger = KotlinLogging.logger { }

  override lateinit var db: Database
    private set

  override fun connect() {
    db = Database.connect("jdbc:h2:file:./db.h2")

    logger.info { "Connected to database: ${db.url}" }

    transaction {
      SchemaUtils.create(Users)

      val users = Users.selectAll()
      logger.info { "Registered users: ${users.count()}" }

      if(users.none { user -> user[Users.username] == "roflanebalo" }) {
        Users.insert {
          it[username] = "roflanebalo"
          it[password] = "test"
          it[score] = 12345
          it[crystals] = 54321
        }
      }
    }
  }
}
