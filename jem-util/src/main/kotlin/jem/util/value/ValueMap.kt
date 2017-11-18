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

package jem.util.value

import jem.util.release
import jem.util.retain

private typealias VariantMap = Map<String, Any>
private typealias VariantEntry = Map.Entry<String, Any>

interface ValueValidator {
    fun validate(name: String, value: Any)
}

class ValueMap(private val validator: ValueValidator? = null) : Iterable<VariantEntry>, Cloneable {
    private var map = hashMapOf<String, Any>()

    val size get() = map.size

    val names get() = map.keys

    fun isEmpty() = map.isEmpty()

    fun isNotEmpty() = map.isNotEmpty()

    operator fun set(name: String, value: Any): Any? {
        require(name.isNotEmpty()) { "name cannot be empty" }
        validator?.validate(name, value)
        val old = map.put(name, value)
        value.retain()
        old.release()
        return old
    }

    fun update(map: VariantMap) {
        map.forEach { set(it.key, it.value) }
    }

    fun update(other: ValueMap) {
        update(other.map)
    }

    operator fun contains(name: String) = name.isNotEmpty() && name in map

    operator fun get(name: String) = if (name.isEmpty()) null else map[name]

    fun remove(name: String) = if (name.isEmpty()) null else map.remove(name)?.release()

    fun clear() {
        map.onEach { it.value.release() }.clear()
    }

    override fun iterator(): Iterator<VariantEntry> = map.entries.iterator()

    override fun toString(): String = map.toString()

    @Suppress("UNCHECKED_CAST")
    override fun clone(): ValueMap {
        val copy = super.clone() as ValueMap
        copy.map = map.clone() as HashMap<String, Any>
        copy.map.onEach { it.value.retain() }
        return copy
    }
}

