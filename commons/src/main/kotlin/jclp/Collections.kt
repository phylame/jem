package jclp

import java.util.*

inline fun <K, V> Map<K, V>.getOrElse(key: K, nullabe: Boolean = false, default: (K) -> V): V? {
    return get(key) ?: if (!nullabe || key !in this) default(key) else null
}

inline fun <K, V> MutableMap<K, V>.getOrPut(key: K, nullabe: Boolean = false, default: (K) -> V): V? {
    var value = get(key)
    if (value == null && (!nullabe || key !in this)) {
        value = default(key)
        if (nullabe || value != null) {
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

operator fun MutableMap<in String, in String>.plusAssign(props: Properties) {
    putAll(props)
}