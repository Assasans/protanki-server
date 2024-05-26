package jp.assasans.protanki.server.extensions

inline fun <T> ThreadLocal<T>.putIfAbsent(put: () -> T): T {
  var value = get()
  if(value == null) {
    value = put()
    set(value)
  }

  return value
}
