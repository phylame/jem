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

package jem.scj

import jclp.Variants
import jclp.locate
import jclp.text.or
import jclp.walk
import jem.Attributes.getName
import jem.Book
import jem.Chapter
import jem.title
import mala.App
import mala.App.tr
import java.util.*

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
            name.matches("^#([\\-\\d.]+)(\\$.*)?".toRegex()) -> viewChapter(book, name.substring(1), settings)
            name.matches("\\+.*".toRegex()) -> viewExtensions(book, listOf(name.substring(1) or { "all" }), settings)
            else -> viewAttributes(book, listOf(name), settings, false)
        }
    }
}

fun viewAttributes(chapter: Chapter, names: Collection<String>, settings: ViewSettings, showBracket: Boolean) {
    val values = LinkedList<String>()
    for (name in names) {
        when (name) {
            "toc" -> viewToc(chapter, settings)
            "all" -> viewAttributes(chapter, chapter.attributes.names, settings, showBracket)
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
                    viewAttributes(chapter, listOf("size"), settings, showBracket)
                } else {
                    values += tr("view.attrPattern", tr("view.chapterSize"), chapter.size)
                }
            }
            else -> {
                val value: Any? = chapter[name]
                val str = if (value != null) Variants.printable(value) ?: value.toString() else ""
                if (str.isNotEmpty() || !settings.skipEmpty) {
                    values += tr("view.attrPattern", getName(name) ?: name, str)
                }
            }
        }
    }
    if (values.isEmpty()) {
        return
    }
    if (showBracket) {
        println(values.joinToString(settings.separator, "<", ">"))
    } else {
        println(values.joinToString(settings.separator))
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
                    tr("view.extPattern", name, Variants.getType(value) ?: "", Variants.printable(value) ?: value.toString())
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
    val indices = parseIndices(tokens.first()) ?: return
    val names = listOf(if (tokens.size > 1) tokens[1] else "text")
    viewAttributes(locateChapter(chapter, indices) ?: return, names, settings, false)
}

fun viewToc(chapter: Chapter, settings: ViewSettings) {
    println(tr("view.tocLegend", chapter.title))
    val separator = settings.separator
    settings.separator = " "
    chapter.walk { level, index ->
        if (level != 0) {
            val prefix = parent?.tag ?: ""
            val fmt = "%${parent?.size.toString().length}d"
            print("$prefix${fmt.format(index + 1)} ")
            viewAttributes(this, settings.tocNames, settings, true)
            if (isSection) {
                tag = "$prefix${index + 1}${settings.tocIndent}"
            }
        }
    }
    settings.separator = separator
}
