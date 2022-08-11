package jp.assasans.protanki.server.extensions

fun <K, V> Map<K, V>.keyOfOrNull(value: V): K? {
  for(item in this) {
    if(item.value == value) return item.key
  }
  return null
}

fun <K, V> Map<K, V>.keyOf(value: V): K? {
  return keyOfOrNull(value) ?: throw IllegalArgumentException("No such value")
}

fun <K, V> Map<out K?, V>.filterKeysNotNull(): Map<K, V> {
  @Suppress("UNCHECKED_CAST")
  return filter { it.key != null } as Map<K, V>
}

fun <K, V> Map<out K, V?>.filterValuesNotNull(): Map<K, V> {
  @Suppress("UNCHECKED_CAST")
  return filter { it.value != null } as Map<K, V>
}
