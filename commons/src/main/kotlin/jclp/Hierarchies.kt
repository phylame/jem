package jclp

interface Hierarchical<T : Hierarchical<T>> : Iterable<T> {
    val size: Int

    val parent: T?

    operator fun get(index: Int): T
}

open class Hierarchy<T : Hierarchy<T>> : Hierarchical<T> {
    override var parent: T? = null
        protected set

    private val children = ArrayList<T>()

    fun append(item: T) = children.add(ensureSolitary(item))

    operator fun plusAssign(item: T) {
        append(item)
    }

    fun insert(index: Int, item: T) = children.add(index, ensureSolitary(item))

    override val size = children.size

    override fun get(index: Int) = children[index]

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

    fun remove(item: T) = if (item.parent !== this) {
        false
    } else if (children.remove(item)) {
        item.parent = null
        true
    } else {
        false
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
        for (item in children) {
            item.parent = null
        }
        children.clear()
    }

    override fun iterator() = children.iterator()

    private fun ensureSolitary(item: T): T {
        require(item !== this) { "Cannot add self to children list: $item" }
        require(item !== parent) { "Cannot add parent to children list: %$item" }
        require(item.parent == null) { "Item has been in certain parent: $item" }
        @Suppress("UNCHECKED_CAST")
        item.parent = this as T
        return item
    }
}
