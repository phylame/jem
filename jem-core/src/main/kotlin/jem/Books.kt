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

package jem

import jclp.Hierarchy
import jclp.VariantMap
import jclp.flob.Flob
import jclp.log.Log
import jclp.text.Text
import java.util.*

typealias ChapterCleaner = (Chapter) -> Unit

open class Chapter(
        title: String = "",
        var text: Text? = null,
        cover: Flob? = null,
        intro: Text? = null,
        var tag: Any? = null
) : Hierarchy<Chapter>(), Cloneable {
    var attributes = Attributes.newAttributes()
        private set

    init {
        set(TITLE, title)
        if (cover != null) {
            set(COVER, cover)
        }
        if (intro != null) {
            set(INTRO, intro)
        }
    }

    constructor(chapter: Chapter, deepCopy: Boolean) : this() {
        dumpTo(chapter, deepCopy)
    }

    operator fun get(name: String) = attributes[name]

    operator fun contains(name: String) = name in attributes

    operator fun set(name: String, value: Any) = attributes.set(name, value)

    operator fun set(name: String, values: Collection<Any>) = attributes.set(name, values.joinToString(";"))

    fun newChapter(title: String = "", text: Text? = null, cover: Flob? = null, intro: Text? = null): Chapter {
        val chapter = Chapter(title, text, cover, intro)
        append(chapter)
        return chapter
    }

    val isSection get() = size != 0

    fun clear(cleanup: Boolean) {
        if (cleanup) {
            for (chapter in this) {
                chapter.cleanup()
            }
        }
        clear()
    }

    private var isCleaned = false

    private val cleanups = LinkedHashSet<ChapterCleaner>()

    operator fun plusAssign(cleanup: ChapterCleaner) {
        cleanups += cleanup
    }

    operator fun minusAssign(cleanup: ChapterCleaner) {
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

    override fun toString(): String {
        val b = StringBuilder(javaClass.simpleName).append('@').append(attributes)
        if (text != null) {
            b.append(", text")
        }
        if (isSection) {
            b.append(", ").append(size).append(" chapter(s)")
        }
        if (tag != null) {
            b.append(", [").append(tag).append(']')
        }
        return b.toString()
    }

    companion object
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

    override fun toString(): String {
        val b = StringBuilder(super.toString())
        if (extensions.isNotEmpty()) {
            b.append(", #").append(extensions)
        }
        return b.toString()
    }

    companion object
}
