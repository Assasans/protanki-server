package jp.assasans.protanki.server.extensions

inline fun <reified T> Any.cast(): T {
  if(this !is T) throw TypeCastException("Cannot cast ${this::class} to ${T::class}")
  return this
}
