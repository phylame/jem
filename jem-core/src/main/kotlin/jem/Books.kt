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

package jem

import jclp.HierarchySupport
import jclp.ValueMap
import jclp.io.Flob
import jclp.log.Log
import jclp.release
import jclp.retain
import jclp.text.Text
import jem.Attributes.VALUE_SEPARATOR

open class Chapter(
        title: String = "", text: Text? = null, cover: Flob? = null, intro: Text? = null, tag: Any? = null
) : HierarchySupport<Chapter>(), Cloneable {
    var attributes = Attributes.newAttributes()
        private set

    var text: Text? = text
        set(value) {
            field.release()
            field = value.retain()
        }

    var tag: Any? = tag
        set(value) {
            field.release()
            field = value.retain()
        }

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
        @Suppress("LeakingThis")
        chapter.copyTo(this, deepCopy)
    }

    operator fun get(name: String) = attributes[name]

    operator fun contains(name: String) = name in attributes

    operator fun set(name: String, value: Any) = attributes.set(name, value)

    operator fun set(name: String, values: Collection<Any>)
            = attributes.set(name, values.joinToString(VALUE_SEPARATOR))

    val isSection inline get() = size != 0

    fun clear(cleanup: Boolean) {
        if (cleanup) forEach { it.cleanup() }
        clear()
    }

    public override fun clone() = (super.clone() as Chapter).also {
        copyTo(it, true)
    }

    protected open fun copyTo(chapter: Chapter, deepCopy: Boolean) {
        chapter.tag = tag
        chapter.text = text
        chapter.parent = null
        chapter.children = children
        chapter.attributes = attributes
        if (deepCopy) {
            chapter.attributes = attributes.clone()
            val list = ArrayList<Chapter>(children.size)
            children.mapTo(list) { it.clone() }
            chapter.children = list
            text.retain()
            tag.retain()
        }
    }

    private var isCleaned = false

    open fun cleanup() {
        isCleaned = true
        clear(true)
        attributes.clear()
        text = null
        tag = null
    }

    protected fun finalize() {
        if (!isCleaned) {
            Log.w("Chapter") { "$title@${hashCode()} is not cleaned" }
        }
    }

    override fun toString(): String {
        val b = StringBuilder(javaClass.simpleName)
        b.append("@0x").append(hashCode().toString(16))
        b.append("%").append(attributes)
        if (text != null) {
            b.append(", text")
        }
        if (isSection) {
            b.append(", ").append(size).append(" chapter(s)")
        }
        return b.toString()
    }
}

fun Chapter.newChapter(title: String = "", text: Text? = null, cover: Flob? = null, intro: Text? = null): Chapter {
    val chapter = Chapter(title, text, cover, intro)
    append(chapter)
    return chapter
}

open class Book : Chapter {
    var extensions = ValueMap()
        private set

    constructor(title: String = "", author: String = "") : super(title) {
        attributes[AUTHOR] = author
    }

    constructor(chapter: Chapter, deepCopy: Boolean) : super(chapter, deepCopy)

    override fun cleanup() {
        super.cleanup()
        extensions.clear()
    }

    override fun copyTo(chapter: Chapter, deepCopy: Boolean) {
        super.copyTo(chapter, deepCopy)
        if (chapter is Book) {
            chapter.extensions = if (deepCopy) extensions.clone() else extensions
        }
    }

    override fun toString(): String {
        val b = StringBuilder(super.toString())
        if (extensions.isNotEmpty()) {
            b.append(", #").append(extensions)
        }
        return b.toString()
    }
}

fun Chapter.asBook() = this as? Book ?: Book(this, false)
