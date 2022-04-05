package jp.assasans.protanki.server.extensions

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.*

fun CoroutineScope.launchDelayed(
  delay: Duration,
  context: CoroutineContext = EmptyCoroutineContext,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit
): Job {
  return launch(context, start) {
    delay(delay.inWholeMilliseconds)
    block()
  }
}
