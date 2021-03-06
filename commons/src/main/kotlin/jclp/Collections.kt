/*
 * Copyright 2015-2017 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jclp

import java.util.*
import java.util.concurrent.ThreadLocalRandom

typealias VariantMap = Map<String, Any>
typealias VariantEntry = Map.Entry<String, Any>

fun <E> List<E>.chooseAny() = get(ThreadLocalRandom.current().nextInt(0, size))

inline fun <E, S : Collection<E>, R> S.ifNotEmpty(block: (S) -> R) = if (isNotEmpty()) block(this) else null

inline fun <K, V, M : Map<K, V>, R> M.ifNotEmpty(block: (M) -> R) = if (isNotEmpty()) block(this) else null

inline fun <K, V> MutableMap<K, V>.getOrSet(key: K, default: (K) -> V?): V? {
    var value = get(key)
    if (value == null && (key !in this)) {
        value = default(key)
        if (value != null) {
            put(key, value)
        }
    }
    return value
}

fun MutableMap<in String, in String>.update(from: Properties) {
    for ((key, value) in from) {
        put(key.toString(), value.toString())
    }
}

enum class WalkEvent {
    NODE, PRE_SECTION, POST_SECTION
}
