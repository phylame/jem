package jem.crawler.ext

import jclp.flob.flobOf
import jclp.io.writeLines
import jclp.setting.Settings
import jclp.text.Text
import jclp.text.remove
import jclp.text.textOf
import jdk.nashorn.api.scripting.ScriptObjectMirror
import jem.*
import jem.crawler.joinText
import jem.epm.EpmFactory
import jem.epm.Parser
import jem.epm.ParserException
import org.jsoup.Jsoup
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.script.ScriptEngineManager

private val charset = Charset.forName("GBK")

class ZXCHMParser : EpmFactory, Parser {
    override val name = "ZhiXuan CHM Parser"

    override val keys = setOf("zxchm")

    override val parser = this

    override fun parse(input: String, arguments: Settings?): Book {
        val book = Book()
        val root = Paths.get(input)
        loadPages(book, root)
        loadInfo(book, root)
        return book
    }

    val engine by lazy { ScriptEngineManager().getEngineByExtension("js") }

    private fun loadInfo(book: Book, root: Path) {
        val path = root.resolve("index1/index.htm")
        if (Files.notExists(path)) {
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
        if (Files.notExists(path)) {
            throw NoSuchFileException(path.toString())
        }
        Files.newBufferedReader(path, charset).use {
            engine.eval(it)
            val pages = engine.get("pages") as? ScriptObjectMirror
            if (pages == null || !pages.isArray) {
                throw ParserException("No pages")
            }
            var section: Chapter? = null
            var index = 0
            for (value in pages.values) {
                if (value !is ScriptObjectMirror || !value.isArray) {
                    throw ParserException("Bad pages")
                }
                @Suppress("UNCHECKED_CAST")
                val items = value.values as List<String>
                val size = items.size
                if (size < 3) {
                    throw ParserException("Bad pages")
                }
                val id = items[0]
                if (index++ == 0) {
                    book.intro = textOf(Jsoup.parse(items[1]).joinText(System.lineSeparator()))
                    if (size > 3) {
                        val src = Jsoup.parse(items[3]).select("img").attr("src")
                        book.cover = flobOf(path.parent.resolve(src))
                    }
                    continue
                }
                val chapter = Chapter(items[1])
                chapter.text = MyText(root.resolve("txt/$id.txt"))
                chapter.words = items[2]
                if (size > 3) {
                    section = book.newChapter(items[3])
                }
                if (section != null) {
                    section.append(chapter)
                } else {
                    book.append(chapter)
                }
            }
        }
    }

    private class MyText(val path: Path) : Text {
        override fun iterator(): Iterator<String> {
            return Files.newBufferedReader(path, charset).useLines {
                it.filterIndexed { i, l -> i != 0 && l.isNotEmpty() }.firstOrNull()?.let {
                    it.substring(17, it.length - 2)
                            .splitToSequence("<p>")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .iterator()
                } ?: Collections.emptyIterator()
            }
        }

        override fun toString() = iterator().asSequence().joinToString(System.lineSeparator())

        override fun writeTo(output: Writer) {
            output.writeLines(iterator(), System.lineSeparator())
        }
    }
}
