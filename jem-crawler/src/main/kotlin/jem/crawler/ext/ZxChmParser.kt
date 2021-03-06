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

package jem.crawler.ext

import jclp.io.Flob
import jclp.io.flobOf
import jclp.io.notExists
import jclp.log.Log
import jclp.setting.Settings
import jclp.text.*
import jdk.nashorn.api.scripting.ScriptObjectMirror
import jem.*
import jem.crawler.joinText
import jem.epm.EpmFactory
import jem.epm.Parser
import jem.epm.ParserException
import org.jsoup.Jsoup
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.script.ScriptEngineManager

private val charset = Charset.forName("GBK")

class ZxChmParser : Parser, EpmFactory {
    private val tagId = javaClass.simpleName

    override val keys = setOf("zxchm")

    override val name = "ZhiXuan CHM"

    override val hasParser = true

    override val parser = this

    private val jsEngine by lazy {
        Log.t(tagId) { "find script engine for JavaScript..." }
        ScriptEngineManager().getEngineByExtension("js")
    }

    override fun parse(input: String, arguments: Settings?): Book {
        val book = Book()
        val root = Paths.get(input)
        loadInfo(book, root)
        loadPages(book, root)
        return book
    }

    private fun loadInfo(book: Book, root: Path) {
        val path = root.resolve("index1/index.htm")
        if (path.notExists) {
            throw NoSuchFileException(path.toString())
        }
        Files.newInputStream(path).use {
            val doc = Jsoup.parse(it, null, path.toString())
            book.author = doc.select("#bkk font:eq(3)").text().trim().remove("作者：")
            book.title = doc.title()
        }
    }

    private fun loadPages(book: Book, root: Path) {
        val path = root.resolve("js/page.js")
        if (path.notExists) {
            throw NoSuchFileException(path.toString())
        }
        Files.newBufferedReader(path, charset).use {
            Log.t(tagId) { "eval 'page.js'..." }
            jsEngine.eval(it)
            val pages = jsEngine.get("pages")
            if (pages !is ScriptObjectMirror || !pages.isArray) {
                throw ParserException("Not found 'pages' variant")
            }
            var section: Chapter? = null
            pages.values.forEachIndexed { index, value ->
                if (value !is ScriptObjectMirror || !value.isArray) {
                    throw ParserException("Bad value of 'pages'")
                }
                @Suppress("UNCHECKED_CAST")
                val items = value.values as List<String>
                val size = items.size
                if (size < 3) {
                    throw ParserException("Bad value of 'pages'")
                }
                val id = items[0]
                if (index == 0 && size == 4) {
                    book.intro = textOf(Jsoup.parse(items[1]).joinText(System.lineSeparator()))
                    if (size > 3) {
                        book.cover = getImage(items[3], path.parent)
                    }
                } else {
                    val chapter = Chapter(items[1])
                    chapter.text = MyText(root.resolve("txt/$id.txt"))
                    chapter.words = items[2]
                    if (size > 3) {
                        section = book.newChapter(items[3])
                        if (size > 6) {
                            section!!.cover = getImage(items[6], path.parent)
                        }
                    }
                    if (section != null) {
                        section!!.append(chapter)
                    } else {
                        book.append(chapter)
                    }
                }
            }
            val meta = jsEngine.get("hangxing")
            if (meta is ScriptObjectMirror && meta.isArray) {
                val value = meta["0"]
                if (value is ScriptObjectMirror) {
                    @Suppress("UNCHECKED_CAST")
                    (value.values as List<String>).let {
                        book.title = it[0]
                        book.author = it[1]
                        book.intro = getText(it[2])
                    }
                }
            }
        }
    }

    private fun getImage(html: String, path: Path): Flob {
        return flobOf(path.resolve(Jsoup.parse(html).select("img").attr("src")))
    }

    private fun getText(html: String): Text {
        return textOf(html.split("<Br>").joinToString("\n") { it.trim() })
    }

    private class MyText(val path: Path) : IteratorText(TEXT_PLAIN) {
        override fun iterator() = Files.newBufferedReader(path, charset).useLines {
            it.filterIndexed { i, l -> i != 0 && l.isNotEmpty() }.firstOrNull()?.let {
                it.substring(17, it.length - 2)
                        .splitToSequence("<p>")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .iterator()
            } ?: Collections.emptyIterator()
        }
    }
}
