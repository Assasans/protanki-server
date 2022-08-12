package jp.assasans.protanki.server.api

import com.squareup.moshi.Moshi
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import jp.assasans.protanki.server.ISocketServer
import jp.assasans.protanki.server.client.IUserRepository
import jp.assasans.protanki.server.client.Screen

interface IApiServer {
  suspend fun run()
  suspend fun stop()
}

class WebApiServer : IApiServer, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val json: Moshi by inject()
  private val server: ISocketServer by inject()
  private val userRepository: IUserRepository by inject()

  private lateinit var engine: ApplicationEngine

  override suspend fun run() {
    engine = embeddedServer(Netty, port = 8081, host = "0.0.0.0") {
      install(ContentNegotiation) {
        moshi(json)
      }

      routing {
        route("/stats") {
          get("players") {
            val players = server.players.filter { player -> player.active }
            call.respond(
              PlayerStats(
                registered = userRepository.getUserCount(),
                online = players.size,
                screens = Screen.values()
                  .associateWith { screen -> players.count { player -> player.screen == screen } }
              )
            )
          }
        }
      }
    }.start()

    logger.info { "Started web API server" }
  }

  override suspend fun stop() {
    logger.debug { "Stopping Ktor engine..." }
    engine.stop(2000, 3000)

    logger.info { "Stopped web API server" }
  }
}
