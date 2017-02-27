/*
 * Copyright 2014-2017 Peng Wan <phylame@163.com>
 *
 * This file is part of SCJ.
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

package jem.scj.app

import jem.Attributes
import jem.Book
import jem.Chapter
import jem.crawler.CrawlerBook
import jem.crawler.CrawlerConfig
import jem.crawler.CrawlerListenerAdapter
import jem.epm.EpmManager
import jem.epm.util.MakerException
import jem.epm.util.ParserException
import jem.formats.pmab.PmabOutConfig
import jem.kotlin.title
import jem.util.JemException
import jem.util.UnsupportedFormatException
import jem.util.Variants
import jem.util.flob.Flobs
import jem.util.text.Texts
import pw.phylame.commons.io.IOUtils
import pw.phylame.commons.util.DateUtils
import pw.phylame.commons.util.MiscUtils
import pw.phylame.qaf.core.App
import pw.phylame.qaf.core.iif
import pw.phylame.qaf.core.tr
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.System.lineSeparator
import java.text.ParseException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

fun printJemError(e: JemException, input: String, format: String) {
    if (e is ParserException) {
        App.error(tr("error.jem.parse", input, format), e)
    } else if (e is MakerException) {
        App.error(tr("error.jem.make", input, format), e)
    } else if (e is UnsupportedFormatException) {
        App.error(tr("error.jem.unsupported", format), e)
    } else {
        App.error(tr("error.unknown"), e)
    }
}

fun openBook(tuple: InTuple): Book? {
    val args = HashMap(tuple.arguments)
    if (tuple.format == "crawler") {
        var key = "crawler.parse.${CrawlerConfig.LISTENER}"
        if (key !in args) {
            args[key] = object : CrawlerListenerAdapter() {
                override fun textFetched(chapter: Chapter, total: Int, current: Int) {
                    println("$current/$total: ${chapter.title}")
                }
            }
        }
    }
    try {
        return EpmManager.readBook(tuple.input, tuple.format, args)
    } catch (e: FileNotFoundException) {
        App.error(tr("error.input.notExists", tuple.input))
    } catch (e: IOException) {
        App.error(tr("error.loadFile", tuple.input), e)
    } catch (e: JemException) {
        printJemError(e, tuple.input, tuple.format)
    }
    return null
}

fun setAttributes(chapter: Chapter, attributes: Map<String, *>): Boolean {
    for ((k, v) in attributes) {
        val str = v.toString()
        if (str.isEmpty()) {
            chapter.attributes.remove(k)
            continue
        }
        val value: Any?
        when (Attributes.typeOf(k)) {
            Variants.FLOB -> value = try {
                Flobs.forURL(IOUtils.resourceFor(str, null), null)
            } catch (e: IOException) {
                App.error(tr("sci.attribute.file.invalid", str), e)
                return false
            }
            Variants.DATETIME -> value = try {
                DateUtils.parse(str, DATE_FORMAT)
            } catch (e: ParseException) {
                App.error(tr("sci.attribute.date.invalid", str))
                return false
            }
            Variants.TEXT -> value = Texts.forString(str, Texts.PLAIN)
            Variants.LOCALE -> value = MiscUtils.parseLocale(str)
            Variants.INTEGER -> value = try {
                str.toInt()
            } catch (e: NumberFormatException) {
                App.error(tr("sci.attribute.integer.invalid", str))
                return false
            }
            Variants.REAL -> value = try {
                str.toDouble()
            } catch (e: NumberFormatException) {
                App.error(tr("sci.attribute.real.invalid", str))
                return false
            }
            Variants.BOOLEAN -> value = str.toBoolean()
            else -> value = str
        }
        if (value != null) {
            chapter.attributes.set(k, value)
        }
    }
    return true
}

fun setExtension(book: Book, extensions: Map<String, *>) {
    for ((k, v) in extensions) {
        val str = v.toString()
        if (str.isEmpty()) {
            book.extensions.remove(k)
        } else {
            book.extensions.set(k, str)
        }
    }
}

fun prepareArguments(tuple: OutTuple): MutableMap<String, Any> {
    val key = "pmab.make." + PmabOutConfig.ZIP_COMMENT
    val args = HashMap(tuple.arguments)
    if (key !in args) {
        args[key] = "generated by $NAME v$VERSION"
    }
    return args
}

fun prepareBook(book: Book, tuple: OutTuple): Book? = if (setAttributes(book, tuple.attributes)) {
    setExtension(book, tuple.extensions)
    book
} else null

var isTaskPool: Boolean = false
val taskPool: ExecutorService by lazy {
    isTaskPool = true
    Executors.newFixedThreadPool(Math.max(48, Runtime.getRuntime().availableProcessors() * 16))
}

fun saveBook(book: Book, tuple: OutTuple): String? {
    prepareBook(book, tuple) ?: return null
    val output = tuple.output.iif(tuple.output.isDirectory) { File(it, "${book.title}.${tuple.format}") }
    try {
        if (book is CrawlerBook) {
            book.initTexts(taskPool, false)
            isTaskPool = true
        }
        EpmManager.writeBook(book, output, tuple.format, prepareArguments(tuple))
        return output.path
    } catch (e: IOException) {
        App.error(tr("error.saveFile", tuple.output), e)
    } catch (e: JemException) {
        printJemError(e, tuple.output.path, tuple.format)
    }
    return null
}

fun convertBook(inTuple: InTuple, outTuple: OutTuple): Boolean {
    val book = openBook(inTuple) ?: return false
    val path = saveBook(book, outTuple)
    if (path != null) {
        println(path)
    }
    book.cleanup()
    return true
}

fun joinBook(tuple: OutTuple): Boolean {
    val book = Book()
    SCI.consumeInputs(object : ConsumerCommand {
        override fun consume(tuple: InTuple): Boolean {
            val sub = openBook(tuple)
            if (sub != null) {
                book.append(sub)
            }
            return true
        }
    })
    val path = saveBook(book, tuple)
    if (path != null) {
        println(path)
    }
    book.cleanup()
    return true
}

private fun parseIndexes(str: String): IntArray? {
    val parts = str.split("\\.".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
    val indices = IntArray(parts.size)
    for (i in parts.indices) {
        val part = parts[i]
        try {
            var n = part.toInt()
            if (n == 0) {
                App.error(tr("sci.invalidIndexes", str))
                return null
            }
            if (n > 0) {
                --n
            }
            indices[i] = n
        } catch (e: NumberFormatException) {
            App.error(tr("sci.invalidIndexes", str), e)
            return null
        }

    }
    return indices
}

fun extractBook(inTuple: InTuple, index: String, outTuple: OutTuple): Boolean {
    val book = openBook(inTuple) ?: return false
    val indexes = parseIndexes(index)
    if (indexes == null) {
        book.cleanup()
        return false
    }
    val chapter = MiscUtils.locate(book, indexes)
    if (chapter == null) {
        book.cleanup()
        return false
    }

    val outBook = Book(chapter)
    if (!chapter.isSection) {
        outBook.append(Chapter(tr("sci.contentTitle"), chapter.text))
    }

    val path = saveBook(outBook, outTuple)
    if (path != null) {
        println(path)
    }

    outBook.cleanup()
    book.cleanup()
    return true
}

fun viewBook(inTuple: InTuple, outTuple: OutTuple, keys: Array<String>): Boolean {
    val book = openBook(inTuple) ?: return false
    if (prepareBook(book, outTuple) == null) {
        book.cleanup()
        return false
    }

    var state = true
    for (key in keys) {
        if (key == VIEW_EXTENSION) {
            state = viewExtension(book, book.extensions.names()) && state
        } else if (key.matches(VIEW_CHAPTER.toRegex())) {
            state = viewChapter(book, key) && state
        } else if (key.matches(VIEW_ITEM.toRegex())) {
            val names = arrayOf(key.replaceFirst("item\\$".toRegex(), ""))
            state = viewExtension(book, names) && state
        } else {
            viewAttribute(book, arrayOf(key), lineSeparator(), false, false)
        }
    }

    book.cleanup()
    return state
}

private fun walkTree(chapter: Chapter, prefix: String, keys: Array<String>,
                     showAttributes: Boolean, showOrder: Boolean, indent: String, showBrackets: Boolean) {
    print(prefix)
    if (showAttributes) {
        viewAttribute(chapter, keys, ", ", showBrackets, true)
    }
    var order = 1
    val format = "%" + chapter.size().toString().length + "d"
    for (sub in chapter) {
        var str = prefix
        if (showAttributes) {
            str += indent
        }
        if (showOrder) {
            str += String.format(format, order++) + " "
        }
        walkTree(sub, str, keys, true, showOrder, indent, showBrackets)
    }
}

private fun viewToc(chapter: Chapter, keys: Array<String>, indent: String, showOrder: Boolean, showBrackets: Boolean) {
    println(tr("sci.view.tocTitle", chapter.title))
    walkTree(chapter, "", keys, false, showOrder, indent, showBrackets)
}

private fun viewAttribute(chapter: Chapter, keys: Array<String>, sep: String, showBrackets: Boolean, ignoreEmpty: Boolean) {
    val lines = LinkedList<String>()
    for (key in keys) {
        if (key == VIEW_ALL) {
            viewAttribute(chapter, chapter.attributes.names(), sep, showBrackets, true)
        } else if (key == VIEW_TOC) {
            viewToc(chapter, arrayOf(Attributes.TITLE, Attributes.COVER), AppConfig.tocIndent, true, true)
        } else if (key == VIEW_TEXT) {
            val text = chapter.text
            if (text == null) {
                println()
                continue
            }
            println(text.text)
        } else if (key == VIEW_NAMES) {
            val names = LinkedList<String>()
            Collections.addAll(names, *chapter.attributes.names())
            Collections.addAll(names, VIEW_TEXT, VIEW_SIZE, VIEW_ALL)
            if (chapter.isSection) {
                names.add(VIEW_TOC)
            }
            if (chapter is Book) {
                names.add(VIEW_EXTENSION)
            }
            println(names.joinToString(", "))
        } else if (key == VIEW_SIZE && !chapter.attributes.contains(VIEW_SIZE)) {
            val str = Integer.toString(chapter.size())
            if (!str.isEmpty()) {
                lines.add(tr("sci.view.attributeFormat", key, str))
            }
        } else {
            val value = chapter.attributes.get(key, null as Any?)
            val str = if (value != null) Variants.printable(value) ?: value.toString() else ""
            if (str.isNotEmpty() || !ignoreEmpty) {
                lines.add(tr("sci.view.attributeFormat", "${Attributes.titleOf(key) ?: key.capitalize()}($key)", str))
            }
        }
    }
    if (lines.isEmpty()) {
        return
    }
    if (showBrackets) {
        println("<" + lines.joinToString(sep) + ">")
    } else {
        println(lines.joinToString(sep))
    }
}

private fun viewExtension(book: Book, names: Array<String>): Boolean {
    var state = true
    for (name in names) {
        val value = book.extensions.get(name, null)
        if (value == null) {
            App.error(tr("error.view.notFoundItem", name))
            state = false
        } else {
            println(tr("sci.view.extensionFormat", name, Variants.typeOf(value), Variants.printable(value) ?: value.toString()))
        }
    }
    return state
}

private fun viewChapter(book: Book, name: String): Boolean {
    val parts = name.replaceFirst("chapter", "").split("$").dropLastWhile { it.isEmpty() }.toTypedArray()
    val index = parts[0]
    val key = if (parts.size > 1) parts[1] else VIEW_TEXT
    val indexes = parseIndexes(index) ?: return false
    try {
        viewAttribute(MiscUtils.locate(book, indexes), arrayOf(key), lineSeparator(), false, false)
        return true
    } catch (e: IndexOutOfBoundsException) {
        App.error(tr("error.view.notFoundChapter", index, book.title), e)
        return false
    }
}
