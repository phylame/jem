package jclp

inline fun <K, V> Map<K, V>.getOrElse(key: K, creator: (K) -> V): V? {
    return get(key) ?: creator(key)
}

inline fun <K, V> MutableMap<K, V>.getOrPut(key: K, creator: (K) -> V): V? {
    val value = get(key)
    return if (value == null) {
        val answer = creator(key)
        put(key, answer)
        answer
    } else {
        value
    }
}