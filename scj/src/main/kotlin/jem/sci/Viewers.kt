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

package jem.sci

import jclp.TypeManager
import jclp.WalkEvent
import jclp.locate
import jclp.text.ifNotEmpty
import jclp.text.or
import jclp.walkTree
import jem.Attributes.getTitle
import jem.Book
import jem.Chapter
import jem.title
import mala.App
import mala.App.tr
import java.util.*
import kotlin.text.isNotEmpty
import kotlin.text.toInt

data class ViewSettings(var separator: String, var skipEmpty: Boolean, var tocIndent: String, var tocNames: Collection<String>)

fun parseIndices(token: String) = try {
    token.split(".").filter(String::isNotEmpty).map(String::toInt)
} catch (e: NumberFormatException) {
    App.error(tr("err.view.badIndex", token), e)
    null
}

fun locateChapter(chapter: Chapter, indices: Collection<Int>) = try {
    chapter.locate(indices)
} catch (e: Exception) {
    App.error(tr("err.view.noChapter", indices), e)
    null
}

fun viewBook(book: Book, names: Collection<String>, settings: ViewSettings) {
    for (name in names) {
        when {
            name.startsWith("#") -> viewChapter(book, name.substring(1), settings)
            name.startsWith("+") -> viewExtensions(book, listOf(name.substring(1) or { "all" }), settings)
            else -> viewAttributes(book, listOf(name), settings, false)
        }
    }
}

fun viewAttributes(chapter: Chapter, names: Collection<String>, settings: ViewSettings, showBracket: Boolean, separator: String = "") {
    val values = LinkedList<String>()
    for (name in names) {
        when (name) {
            "toc" -> viewToc(chapter, settings)
            "all" -> viewAttributes(chapter, chapter.attributes.names, settings, showBracket, separator)
            "text" -> values += tr("view.attrPattern", tr("view.chapterText"), chapter.text ?: "")
            "names" -> {
                val keys = chapter.attributes.names.toMutableSet()
                Collections.addAll(keys, "text", "size", "all")
                if (chapter.isSection) {
                    keys += "toc"
                }
                println(keys.joinToString(", "))
            }
            "size" -> {
                if ("size" in chapter.attributes) {
                    viewAttributes(chapter, listOf("size"), settings, showBracket, separator)
                } else {
                    values += tr("view.attrPattern", tr("view.chapterSize"), chapter.size)
                }
            }
            else -> {
                val value: Any? = chapter[name]
                val str = if (value != null) TypeManager.printable(value) ?: value.toString() else ""
                if (str.isNotEmpty() || !settings.skipEmpty) {
                    values += tr("view.attrPattern", getTitle(name) ?: name, str)
                }
            }
        }
    }
    if (values.isEmpty()) {
        return
    }
    if (showBracket) {
        println(values.joinToString(separator or { settings.separator }, "<", ">"))
    } else {
        println(values.joinToString(separator or { settings.separator }))
    }
}

fun viewExtensions(book: Book, names: Collection<String>, settings: ViewSettings) {
    val values = LinkedList<String>()
    for (name in names) {
        when (name) {
            "all" -> viewExtensions(book, book.extensions.names, settings)
            "names" -> println((book.extensions.names + "all").joinToString(", "))
            else -> {
                val value = book.extensions[name]
                values += if (value == null) {
                    tr("view.extPattern", name, "", "")
                } else {
                    tr("view.extPattern", name, TypeManager.getType(value) ?: value.javaClass.name, TypeManager.printable(value) ?: value.toString())
                }
            }
        }
    }
    if (values.isNotEmpty()) {
        println(values.joinToString(settings.separator))
    }
}

fun viewChapter(chapter: Chapter, name: String, settings: ViewSettings) {
    val tokens = name.split("$")
    val indices = parseIndices(tokens.first())?.map { it - 1 } ?: return
    val names = listOf(if (tokens.size > 1) tokens[1] else "text")
    viewAttributes(locateChapter(chapter, indices) ?: return, names, settings, false)
}

fun viewToc(chapter: Chapter, settings: ViewSettings) {
    println(tr("view.tocLegend", chapter.title))
    val indent = settings.tocIndent
    val tocNames = settings.tocNames
    val prefixes = LinkedList<String>()
    chapter.walkTree { stub, event, _, index ->
        when (event) {
            WalkEvent.NODE, WalkEvent.PRE_SECTION -> {
                if (stub !== chapter) {
                    val order = index + 1
                    val fmt = "%0${stub.parent!!.size.toString().length}d"
                    print("${prefixes.joinToString(indent).ifNotEmpty { it + indent } ?: ""}${fmt.format(order)} ")
                    viewAttributes(stub, tocNames, settings, true, " ")
                    if (event == WalkEvent.PRE_SECTION) {
                        prefixes.offerLast(order.toString())
                    }
                }
            }
            WalkEvent.POST_SECTION -> {
                prefixes.pollLast()
            }
        }
    }
}
