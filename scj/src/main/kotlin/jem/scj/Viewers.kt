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

data class ViewConfig(var separator: String, var skipEmpty: Boolean, var tocIndent: String, var tocNames: Collection<String>)

fun parseIndices(token: String) = try {
    token.split(".").filter(String::isNotEmpty).map(String::toInt)
} catch (e: NumberFormatException) {
    App.error(tr("err.view.badIndex", token), e)
    null
}

fun locateChapter(chapter: Chapter, indices: Collection<Int>) = try {
    chapter.locate(indices)
} catch (e: IndexOutOfBoundsException) {
    App.error(tr("err.view.noChapter", indices), e)
    null
}

fun viewBook(book: Book, names: Collection<String>, config: ViewConfig) {
    for (name in names) {
        when {
            name.matches("^#([\\-\\d.]+)(\\$.*)?".toRegex()) -> viewChapter(book, name, config)
            name.matches("\\+.*".toRegex()) -> viewExtensions(book, listOf(name.replaceFirst("+", "") or { "all" }), config)
            else -> viewAttributes(book, listOf(name), config, false)
        }
    }
}

fun viewAttributes(chapter: Chapter, names: Collection<String>, config: ViewConfig, showBracket: Boolean) {
    val values = LinkedList<String>()
    for (name in names) {
        when (name) {
            "toc" -> viewToc(chapter, config)
            "all" -> viewAttributes(chapter, chapter.attributes.names, config, showBracket)
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
                    viewAttributes(chapter, listOf("size"), config, showBracket)
                } else {
                    values += tr("view.attrPattern", tr("view.chapterSize"), chapter.size)
                }
            }
            else -> {
                val value: Any? = chapter[name]
                val str = if (value != null) Variants.printable(value) ?: value.toString() else ""
                if (str.isNotEmpty() || !config.skipEmpty) {
                    values += tr("view.attrPattern", getName(name) ?: name, str)
                }
            }
        }
    }
    if (values.isEmpty()) {
        return
    }
    if (showBracket) {
        println(values.joinToString(config.separator, "<", ">"))
    } else {
        println(values.joinToString(config.separator))
    }
}

fun viewExtensions(book: Book, names: Collection<String>, config: ViewConfig) {
    val values = LinkedList<String>()
    for (name in names) {
        when (name) {
            "all" -> viewExtensions(book, book.extensions.names, config)
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
        println(values.joinToString(config.separator))
    }
}

fun viewChapter(chapter: Chapter, name: String, config: ViewConfig) {
    val tokens = name.replaceFirst("#", "").split("$")
    val indices: Collection<Int> = parseIndices(tokens.first()) ?: return
    val names = listOf(if (tokens.size > 1) tokens[1] else "text")
    viewAttributes(locateChapter(chapter, indices) ?: return, names, config, false)
}

fun viewToc(chapter: Chapter, config: ViewConfig) {
    println(tr("view.tocLegend", chapter.title))
    val separator = config.separator
    config.separator = " "
    chapter.walk { level, index ->
        if (level != 0) {
            val prefix = parent?.tag ?: ""
            val fmt = "%${parent?.size.toString().length}d"
            print("$prefix${fmt.format(index + 1)} ")
            viewAttributes(this, config.tocNames, config, true)
            if (isSection) {
                tag = "$prefix${index + 1}${config.tocIndent}"
            }
        }
    }
    config.separator = separator
}
