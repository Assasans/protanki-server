package jp.assasans.protanki.server.extensions

// TODO(Assasans): Find a way to not specify the type of the Iterable<T> in calls
inline fun <T : Any, reified I : T> Iterable<T>.singleOf(predicate: (T) -> Boolean = { true }): I =
  single { it is I && predicate(it) }.cast()

inline fun <T : Any, reified I : T> Iterable<T>.singleOrNullOf(predicate: (T) -> Boolean = { true }): I? =
  singleOrNull { it is I && predicate(it) }?.cast()

/**
 * Returns the single element matching the given [predicate],
 * returns null if there is no matching elements,
 * or throws exception if there is more than one matching element.
 */
inline fun <T> Iterable<T>.singleOrNullOrThrow(predicate: (T) -> Boolean = { true }): T? {
  val items = filter(predicate)
  if(items.isEmpty()) return null
  return items.single()
}
