package jp.assasans.protanki.server.extensions

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect

suspend inline fun <T> StateFlow<T>.collectWithCurrent(crossinline action: suspend (value: T) -> Unit) {
  action(value)
  collect(action)
}
