/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.scj.app

import pw.phylame.jem.core.Attributes
import pw.phylame.jem.core.Book
import pw.phylame.jem.core.Chapter
import pw.phylame.jem.core.Jem
import pw.phylame.jem.epm.Helper
import pw.phylame.jem.epm.Registry
import pw.phylame.jem.epm.util.MakerException
import pw.phylame.jem.epm.util.ParserException
import pw.phylame.jem.formats.pmab.PmabOutConfig
import pw.phylame.jem.util.JemException
import pw.phylame.jem.util.UnsupportedFormatException
import pw.phylame.jem.util.Variants
import pw.phylame.jem.util.flob.Flobs
import pw.phylame.jem.util.text.Text
import pw.phylame.jem.util.text.Texts
import pw.phylame.qaf.core.App
import pw.phylame.qaf.core.tr
import pw.phylame.ycl.format.Converters
import pw.phylame.ycl.io.IOUtils
import pw.phylame.ycl.util.DateUtils
import java.io.File
import java.io.IOException
import java.lang.System.lineSeparator
import java.text.ParseException
import java.util.*

fun propertiesToMap(prop: Properties?): MutableMap<String, Any> {
    val map = HashMap<String, Any>()
    if (prop != null) {
        for ((k, v) in prop) {
            map.put(k.toString(), v)
        }
    }
    return map
}

class InValues(context: Map<String, Any>) {
    lateinit var file: File
    var format: String? = null
    var arguments: Map<String, Any>
    var attributes: Map<String, Any>
    var extensions: Map<String, Any>

    init {
        format = context["inFormat"] as? String
        arguments = propertiesToMap(context["inArguments"] as? Properties)
        attributes = propertiesToMap(context["outAttributes"] as? Properties)
        extensions = propertiesToMap(context["outExtensions"] as? Properties)
    }
}

class OutValues(context: Map<String, Any>) {
    var file: File
    var format: String
    var arguments: MutableMap<String, Any>? = null

    init {
        file = File(context.getOrElse("output") { "." } as String)
        format = context.getOrElse("outFormat") { AppConfig.outputFormat } as String
        arguments = propertiesToMap(context["outArguments"] as? Properties)
    }
}

fun printJemError(e: JemException, file: File, format: String) {
    if (e is ParserException) {
        App.error(tr("error.jem.parse", file, format.toUpperCase()), e)
    } else if (e is MakerException) {
        App.error(tr("error.jem.make", file, format.toUpperCase()), e)
    } else if (e is UnsupportedFormatException) {
        App.error(tr("error.jem.unsupported", format), e)
    }
}

fun openBook(option: InValues): Book? {
    var book: Book? = null
    try {
        book = Helper.readBook(option.file, option.format, option.arguments)
    } catch (e: IOException) {
        App.error(tr("error.loadFile", option.file), e)
    } catch (e: JemException) {
        printJemError(e, option.file, option.format!!)
    }

    return book
}

fun setAttributes(chapter: Chapter, attributes: Map<String, Any>): Boolean {
    for ((k, v) in attributes) {
        val str = v.toString()
        if (str.isEmpty()) {
            chapter.attributes.remove(k)
            continue
        }
        val value: Any?
        when (k) {
            Attributes.COVER -> {
                try {
                    value = Flobs.forURL(IOUtils.resourceFor(str, null), null)
                } catch (e: IOException) {
                    App.error(tr("sci.attribute.cover.invalid", str), e)
                    return false
                }

            }
            Attributes.DATE -> {
                try {
                    value = DateUtils.parse(str, DATE_FORMAT)
                } catch (e: ParseException) {
                    App.error(tr("sci.attribute.date.invalid", str))
                    return false
                }

            }
            Attributes.INTRO -> value = Texts.forString(str, Text.PLAIN)
            Attributes.LANGUAGE -> value = Converters.parse(str, Locale::class.java)
            else -> value = str
        }
        if (value != null) {
            chapter.attributes.put(k, value)
        }
    }
    return true
}

fun setExtension(book: Book, extensions: Map<String, Any>) {
    for ((k, v) in extensions) {
        val str = v.toString()
        if (str.isEmpty()) {
            book.extensions.remove(k)
        } else {
            book.extensions.put(k, str)
        }
    }
}

fun saveBook(book: Book, option: OutValues): String? {
    var output = option.file
    if (output.isDirectory) {
        output = File(output, "${Attributes.getTitle(book)}.${option.format}")
    }
    if (option.arguments != null) {
        val key = "pmab.make." + PmabOutConfig.ZIP_COMMENT
        if (key !in option.arguments!!) {
            option.arguments!![key] = "generated by $NAME v$VERSION"
        }
    }
    var path: String? = null
    try {
        Helper.writeBook(book, output, option.format, option.arguments)
        path = output.path
    } catch (e: IOException) {
        App.error(tr("error.saveFile", option.file), e)
    } catch (e: JemException) {
        printJemError(e, option.file, option.format)
    }

    return path
}

fun convertBook(inValues: InValues, outValues: OutValues): Boolean {
    val book = openBook(inValues) ?: return false
    if (!setAttributes(book, inValues.attributes)) {
        book.cleanup()
        return false
    }
    setExtension(book, inValues.extensions)

    val path = saveBook(book, outValues)
    if (path != null) {
        println(path)
    }

    book.cleanup()
    return true
}

fun joinBook(inputs: Array<String>, inValues: InValues, outValues: OutValues): Boolean {
    val book = Book()
    val initFormat = inValues.format
    for (input in inputs) {
        val file = File(input)
        // check it exists
        if (!file.exists()) {
            App.error(tr("error.input.notExists", input))
            continue
        }

        val format = initFormat ?: Helper.formatOfExtension(input)
        if (!checkInputFormat(format)) {
            continue
        }

        inValues.file = file
        inValues.format = format
        val sub = openBook(inValues)
        if (sub != null) {
            book.append(sub)
        }
    }
    if (!setAttributes(book, inValues.attributes)) {
        book.cleanup()
        return false
    }
    setExtension(book, inValues.extensions)

    val path = saveBook(book, outValues)
    if (path != null) {
        println(path)
    }

    book.cleanup()
    inValues.format = initFormat
    return true
}

private fun parseIndexes(str: String): IntArray? {
    val parts = str.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val indices = IntArray(parts.size)
    for (i in parts.indices) {
        val part = parts[i]
        try {
            var n = Integer.parseInt(part)
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

fun extractBook(inValues: InValues, index: String, outValues: OutValues): Boolean {
    val book = openBook(inValues) ?: return false
    val indexes = parseIndexes(index)
    if (indexes == null) {
        book.cleanup()
        return false
    }
    val chapter = Jem.locate(book, indexes)
    if (chapter == null) {
        book.cleanup()
        return false
    }

    val outBook = Book(chapter)
    if (!chapter.isSection) {
        outBook.append(Chapter(tr("sci.contentTitle"), chapter.text))
    }

    if (!setAttributes(outBook, inValues.attributes)) {
        outBook.cleanup()
        book.cleanup()
        return false
    }
    setExtension(outBook, inValues.extensions)

    val path = saveBook(outBook, outValues)
    if (path != null) {
        println(path)
    }

    outBook.cleanup()
    book.cleanup()
    return true
}

fun viewBook(option: InValues, keys: Array<String>): Boolean {
    val book = openBook(option) ?: return false
    if (!setAttributes(book, option.attributes)) {
        book.cleanup()
        return false
    }
    setExtension(book, option.extensions)

    var state = true
    for (key in keys) {
        if (key == VIEW_EXTENSION) {
            val names = book.extensions.keys()
            state = viewExtension(book, names) && state
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
                     showAttributes: Boolean, showOrder: Boolean,
                     indent: String, showBrackets: Boolean) {
    print(prefix)
    if (showAttributes) {
        viewAttribute(chapter, keys, ", ", showBrackets, true)
    }
    var order = 1
    val size = chapter.size().toString()
    val format = "%" + size.length + "d"
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

private fun viewToc(chapter: Chapter, keys: Array<String>, indent: String,
                    showOrder: Boolean, showBrackets: Boolean) {
    println(tr("sci.view.tocTitle", Attributes.getTitle(chapter)))
    walkTree(chapter, "", keys, false, showOrder, indent, showBrackets)
}

private fun viewAttribute(chapter: Chapter, keys: Array<String>, sep: String,
                          showBrackets: Boolean, ignoreEmpty: Boolean) {
    val lines = LinkedList<String>()
    for (key in keys) {
        if (key == VIEW_ALL) {
            val names = chapter.attributes.keys()
            viewAttribute(chapter, names, sep, showBrackets, true)
        } else if (key == VIEW_TOC) {
            val names = arrayOf(Attributes.TITLE, Attributes.COVER)
            viewToc(chapter, names, AppConfig.tocIndent, true, true)
        } else if (key == VIEW_TEXT) {
            val text = chapter.text
            if (text == null) {
                println()
                continue
            }
            try {
                println(text.text)
            } catch (e: Exception) {
                App.error(tr("error.view.fetchContent", Attributes.getTitle(chapter)), e)
            }

        } else if (key == VIEW_NAMES) {
            val names = LinkedList<String>()
            Collections.addAll(names, *chapter.attributes.keys())
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
            val value = chapter.attributes.get(key, null)
            val str: String
            if (value != null) {
                str = Variants.format(value)
            } else {
                str = ""
            }

            if (!str.isEmpty() || !ignoreEmpty) {
                lines.add(tr("sci.view.attributeFormat", key, str))
            }
        }
    }
    if (lines.size === 0) {
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
            val str = Variants.format(value)
            println(tr("sci.view.extensionFormat", name, Variants.typeOf(value), str))
        }
    }
    return state
}

private fun viewChapter(book: Book, name: String): Boolean {
    val parts = name.replaceFirst("chapter".toRegex(), "").split("\\$".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val index = parts[0]
    val key: String
    if (parts.size > 1) {
        key = parts[1]
    } else {
        key = VIEW_TEXT
    }
    val indexes = parseIndexes(index) ?: return false
    try {
        viewAttribute(Jem.locate(book, indexes), arrayOf(key),
                lineSeparator(), false, false)
        return true
    } catch (e: IndexOutOfBoundsException) {
        App.error(tr("error.view.notFoundChapter", index, Attributes.getTitle(book)), e)
        return false
    }
}
