package jem

import jclp.Hierarchy
import jclp.log.Log
import jclp.value.VariantMap
import jclp.value.Text
import java.util.*

private typealias Cleaner = (Chapter) -> Unit

open class Chapter(title: String = "", var text: Text? = null, var tag: Any? = null) : Hierarchy<Chapter>(), Cloneable {
    var attributes = Attributes.newAttributes()
        private set

    init {
        set(TITLE, title)
    }

    constructor(chapter: Chapter, deepCopy: Boolean) : this() {
        dumpTo(chapter, deepCopy)
    }

    operator fun get(name: String) = attributes[name]

    operator fun contains(name: String) = name in attributes

    operator fun set(name: String, value: Any) = attributes.set(name, value)

    operator fun set(name: String, values: Collection<Any>) = attributes.set(name, values.joinToString(";"))

    fun newChapter(title: String): Chapter {
        val chapter = Chapter(title)
        append(chapter)
        return chapter
    }

    val isSection get() = size != 0

    fun chapterAt(index: Int) = get(index)

    fun clear(cleanup: Boolean) {
        if (cleanup) {
            for (chapter in this) {
                chapter.cleanup()
            }
        }
        clear()
    }

    private var isCleaned = false

    private val cleanups = LinkedHashSet<Cleaner>()

    fun addCleanup(cleanup: Cleaner) {
        cleanups += cleanup
    }

    fun removeCleanup(cleanup: Cleaner) {
        cleanups -= cleanup
    }

    fun cleanup() {
        if (isCleaned) {
            return
        }
        for (cleanup in cleanups) {
            cleanup(this)
        }
        clear(true)
        cleanups.clear()
        attributes.clear()
        isCleaned = true
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        if (!isCleaned) {
            Log.w(javaClass.simpleName) { "chapter ${get(TITLE)} is not cleaned" }
        }
    }

    public override fun clone(): Chapter {
        val copy = super.clone() as Chapter
        dumpTo(copy, true)
        copy.parent = null
        return copy
    }

    protected open fun dumpTo(chapter: Chapter, deepCopy: Boolean) {
        chapter.attributes = attributes.clone()
        if (deepCopy) {
            chapter.clear()
            for (stub in this) {
                chapter.append(stub.clone())
            }
        }
    }

    override fun toString() = "${javaClass.simpleName}:attributes=$attributes, tag=$tag, text=$text"
}

open class Book(title: String = "", author: String = "") : Chapter(title) {
    var extensions = VariantMap()
        private set

    init {
        set(AUTHOR, author)
    }

    constructor(chapter: Chapter, deepCopy: Boolean) : this() {
        dumpTo(chapter, deepCopy)
    }

    override fun dumpTo(chapter: Chapter, deepCopy: Boolean) {
        super.dumpTo(chapter, deepCopy)
        if (chapter is Book) {
            chapter.extensions = extensions.clone()
        }
    }

    override fun toString() = "${super.toString()}, extensions=$extensions"
}
