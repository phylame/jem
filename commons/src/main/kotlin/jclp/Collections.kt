package jclp

import java.util.*

fun MutableList<*>.swap(from: Int, to: Int) = Collections.swap(this, from, to)

inline fun <K, V> MutableMap<K, V>.getOrPut(key: K, default: (K) -> V?): V? {
    var value = get(key)
    if (value == null && (key !in this)) {
        value = default(key)
        if (value != null) {
            put(key, value)
        }
    }
    return value
}

fun MutableMap<in String, in String>.putAll(from: Properties) {
    if (from.isNotEmpty()) {
        for ((key, value) in from) {
            put(key.toString(), value.toString())
        }
    }
}