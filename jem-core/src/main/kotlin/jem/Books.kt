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

import jclp.Hierarchy
import jclp.VariantMap
import jclp.flob.Flob
import jclp.log.Log
import jclp.releaseSelf
import jclp.retainSelf
import jclp.text.Text
import jem.Attributes.VALUE_SEPARATOR

open class Chapter(
        title: String = "", text: Text? = null, cover: Flob? = null, intro: Text? = null, tag: Any? = null
) : Hierarchy<Chapter>(), Cloneable {
    var attributes = Attributes.newAttributes()
        private set

    var text: Text? = text
        set(value) {
            field?.releaseSelf()
            value?.retainSelf()
            field = value
        }

    var tag: Any? = tag
        set(value) {
            field?.releaseSelf()
            value?.retainSelf()
            field = value
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
        chapter.dumpTo(this, deepCopy)
    }

    operator fun get(name: String) = attributes[name]

    operator fun contains(name: String) = name in attributes

    operator fun set(name: String, value: Any) = attributes.set(name, value)

    operator fun set(name: String, values: Collection<Any>) = attributes.set(name, values.joinToString(VALUE_SEPARATOR))

    val isSection get() = size != 0

    fun clear(cleanup: Boolean) {
        if (cleanup) forEach(Chapter::cleanup)
        clear()
    }

    fun cleanup() {
        isCleaned = true
        clear(true)
        attributes.clear()
        text = null
    }

    private var isCleaned = false

    protected fun finalize() {
        if (!isCleaned) {
            Log.w(javaClass.simpleName) { "'$title' is not cleaned" }
        }
    }

    public override fun clone() = (super.clone() as Chapter).also {
        dumpTo(it, true)
    }

    protected open fun dumpTo(chapter: Chapter, deepCopy: Boolean) {
        chapter.tag = tag
        chapter.text = text
        chapter.parent = null
        chapter.children = children
        chapter.attributes = attributes
        if (deepCopy) {
            chapter.attributes = attributes.clone()
            val list = ArrayList<Chapter>(children.size)
            children.forEach { list.add(it.clone()) }
            chapter.children = list
            text?.retainSelf()
            tag?.retainSelf()
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
    var extensions = VariantMap()
        private set

    constructor(title: String = "", author: String = "") : super(title) {
        attributes[AUTHOR] = author
    }

    constructor(chapter: Chapter, deepCopy: Boolean) : super(chapter, deepCopy)

    override fun dumpTo(chapter: Chapter, deepCopy: Boolean) {
        super.dumpTo(chapter, deepCopy)
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
