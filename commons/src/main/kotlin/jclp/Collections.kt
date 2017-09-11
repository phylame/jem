package jclp

inline fun <K, V> Map<K, V>.getOrElse(key: K, creator: (K) -> V) = get(key) ?: creator(key)

inline fun <K, V> MutableMap<K, V>.getOrPut(key: K, creator: (K) -> V) = get(key) ?: creator(key).apply {
    put(key, this)
}
