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

interface Hierarchical<T : Hierarchical<T>> : Iterable<T> {
    val parent: T?

    operator fun get(index: Int): T

    val size: Int
}

val Hierarchical<*>.depth: Int
    get() {
        if (size == 0) return 0
        var depth = 0
        for (item in this) {
            depth = maxOf(depth, item.depth)
        }
        return depth + 1
    }

val Hierarchical<*>.isEmpty inline get() = size == 0

val Hierarchical<*>.isNotEmpty inline get() = size != 0

val Hierarchical<*>.isRoot inline get() = parent == null

val Hierarchical<*>.isNotRoot inline get() = parent != null

fun <T : Hierarchical<T>> T.isAncestor(item: T): Boolean {
    var parent = item.parent
    while (parent != null) {
        if (parent === this) return true
        parent = parent.parent
    }
    return false
}

fun <T : Hierarchical<T>> T.belowOf(top: T? = null): T {
    var parent: T = this
    while (parent.parent != top) {
        parent = parent.parent!!
    }
    return parent
}

val <T : Hierarchical<T>> T.root inline get() = belowOf(null)

fun <T : Hierarchical<T>> T.pathTo(top: T?): List<T> {
    val list = LinkedList<T>()
    var parent: T? = this
    while (parent !== top) {
        list.addFirst(parent)
        parent = parent?.parent
    }
    return list
}

fun <T : Hierarchical<T>> T.toRoot() = pathTo(null)

fun <T : Hierarchical<T>> T.locate(indices: IntArray): T? {
    var item: T? = null
    for (index in indices) {
        item = (item ?: this)[if (index < 0) index + size else index]
    }
    return item
}

fun <T : Hierarchical<T>> T.locate(indices: Collection<Int>): T? {
    var item: T? = null
    for (index in indices) {
        item = (item ?: this)[if (index < 0) index + size else index]
    }
    return item
}

open class HierarchySupport<T : HierarchySupport<T>> : Hierarchical<T> {
    final override var parent: T? = null
        protected set

    protected var children = arrayListOf<T>()

    fun append(item: T) {
        children.add(ensureSolitary(item))
    }

    fun extend(items: Iterable<T>) {
        items.forEach { append(it) }
    }

    operator fun plusAssign(item: T) {
        append(item)
    }

    operator fun plusAssign(items: Iterable<T>) {
        extend(items)
    }

    fun insert(index: Int, item: T) {
        children.add(index, ensureSolitary(item))
    }

    final override val size get() = children.size

    final override fun get(index: Int) = children[index]

    fun indexOf(item: T) = if (item.parent === this) {
        children.indexOfFirst { it === item }
    } else {
        -1
    }

    operator fun contains(item: T) = indexOf(item) != -1

    fun replace(item: T, target: T): Boolean {
        val index = indexOf(item)
        if (index == -1) return false
        replaceAt(index, target)
        return true
    }

    fun replaceAt(index: Int, item: T): T {
        val old = children.set(index, ensureSolitary(item))
        old.parent = null
        return old
    }

    operator fun set(index: Int, item: T) {
        replaceAt(index, item)
    }

    fun swap(from: Int, to: Int) {
        Collections.swap(children, from, to)
    }

    fun remove(item: T): Boolean {
        val index = indexOf(item)
        if (index == -1) return false
        removeAt(index)
        return true
    }

    fun removeAt(index: Int): T {
        val old = children.removeAt(index)
        old.parent = null
        return old
    }

    operator fun minusAssign(item: T) {
        remove(item)
    }

    fun clear() {
        children.onEach { it.parent = null }.clear()
    }

    final override fun iterator() = children.iterator()

    @Suppress("UNCHECKED_CAST")
    private fun ensureSolitary(item: T): T {
        require(item !== this) { "Cannot add self to child list" }
        require(item !== parent) { "Cannot add parent to child list" }
        require(item.parent == null) { "Item has been in certain hierarchy" }
        require(!item.isAncestor(this as T)) { "Cannot add ancestor to child list" }
        item.parent = this
        return item
    }
}
