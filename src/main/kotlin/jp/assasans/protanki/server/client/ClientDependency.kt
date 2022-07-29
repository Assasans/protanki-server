package jp.assasans.protanki.server.client

import kotlinx.coroutines.CompletableDeferred
import mu.KotlinLogging

class ClientDependency(
  val id: Int,
  private val deferred: CompletableDeferred<Unit>
) {
  private val logger = KotlinLogging.logger { }

  suspend fun await() {
    logger.debug { "Waiting for dependency $id to load..." }
    deferred.await()
  }

  fun loaded() {
    deferred.complete(Unit)
    logger.debug { "Marked dependency $id as loaded" }
  }
}
