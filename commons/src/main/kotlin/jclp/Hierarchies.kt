/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

interface Hierarchical<T : Hierarchical<T>> : Iterable<T> {
    val size: Int

    val parent: T?

    operator fun get(index: Int): T
}

fun <T : Hierarchical<T>> T.toRoot(): List<T> {
    val list = LinkedList<T>()
    var parent: T? = this
    while (parent != null) {
        list.addFirst(parent)
        parent = parent.parent
    }
    return list
}

fun <T : Hierarchical<T>> T.locate(indices: IntArray): T? {
    var item: T? = null
    for (index in indices) {
        item = get(if (index < 0) index + size else index)
    }
    return item
}

val Hierarchical<*>.depth: Int
    get() {
        if (size == 0) {
            return 0
        }
        var depth = 0
        for (item in this) {
            depth = maxOf(depth, item.depth)
        }
        return depth + 1
    }

fun <T : Hierarchical<T>> T.locate(indices: Collection<Int>): T? {
    var item: T? = null
    for (index in indices) {
        item = get(if (index < 0) index + size else index)
    }
    return item
}

open class Hierarchy<T : Hierarchy<T>> : Hierarchical<T> {
    final override var parent: T? = null
        protected set

    private val children = ArrayList<T>()

    final override val size get() = children.size

    fun append(item: T) {
        children.add(ensureSolitary(item))
    }

    operator fun plusAssign(item: T) {
        append(item)
    }

    fun insert(index: Int, item: T) {
        children.add(index, ensureSolitary(item))
    }

    final override fun get(index: Int) = children[index]

    fun indexOf(item: T) = if (item.parent === this) children.indexOf(item) else -1

    operator fun contains(item: T) = indexOf(item) != -1

    fun replace(item: T, target: T): Boolean {
        val index = indexOf(item)
        if (index == -1) {
            return false
        }
        replaceAt(index, target)
        return true
    }

    fun replaceAt(index: Int, item: T): T {
        val current = children.set(index, ensureSolitary(item))
        current.parent = null
        return current
    }

    operator fun set(index: Int, item: T) {
        replaceAt(index, item)
    }

    fun remove(item: T) = when {
        item.parent !== this -> false
        children.remove(item) -> {
            item.parent = null
            true
        }
        else -> false
    }

    operator fun minusAssign(item: T) {
        remove(item)
    }

    fun removeAt(index: Int): T {
        val current = children.removeAt(index)
        current.parent = null
        return current
    }

    fun swap(from: Int, to: Int) = children.swap(from, to)

    fun clear() {
        children.onEach { it.parent = null }.clear()
    }

    final override fun iterator() = children.iterator()

    private fun ensureSolitary(item: T): T {
        require(item !== this) { "Cannot add self to children list: $item" }
        require(item !== parent) { "Cannot add parent to children list: %$item" }
        require(item.parent == null) { "Item has been in certain parent: $item" }
        @Suppress("UNCHECKED_CAST")
        item.parent = this as T
        return item
    }
}
