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

fun <E> List<E>.chooseAny() = get(ThreadLocalRandom.current().nextInt(0, size))

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
    for ((key, value) in from) {
        put(key.toString(), value.toString())
    }
}

fun <E : Iterable<E>> E.walk(level: Int = 0, index: Int = 0, firstMode: Boolean = true, block: E.(Int, Int) -> Unit) {
    if (firstMode) {
        block(level, index)
    }
    val lv = level + 1
    forEachIndexed { i, e ->
        e.walk(lv, i, firstMode, block)
    }
    if (!firstMode) {
        block(level, index)
    }
}
