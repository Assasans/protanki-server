package jp.assasans.protanki.server.extensions

// TODO(Assasans): Find a way to not specify the type of the Iterable<T> in calls
inline fun <T, reified I : T> Iterable<T>.singleOf(): I? = this.single { it is I }?.cast()
inline fun <T, reified I : T> Iterable<T>.singleOrNullOf(): I? = this.singleOrNull { it is I }?.cast()

/**
 * Returns the single element matching the given [predicate],
 * returns null if there is no matching elements,
 * or throws exception if there is more than one matching element.
 */
inline fun <T> Iterable<T>.singleOrNullOrThrow(predicate: (T) -> Boolean = { true }): T? {
  val items = this.filter(predicate)
  if(items.isEmpty()) return null
  return items.single()
}
