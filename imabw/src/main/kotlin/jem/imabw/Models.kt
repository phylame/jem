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

package jem.imabw

import javafx.concurrent.Task
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import jclp.EventAction
import jclp.EventBus
import jclp.log.Log
import jclp.text.or
import jclp.text.remove
import jem.Book
import jem.Chapter
import jem.asBook
import jem.epm.*
import jem.imabw.ui.*
import jem.title
import mala.App
import mala.App.tr
import mala.ixin.CommandHandler
import mala.ixin.IxIn
import mala.ixin.init
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

private const val SWAP_SUFFIX = ".swp"

const val TITLE_MODIFIED = 1
const val AUTHOR_MODIFIED = 2
const val ATTRIBUTE_MODIFIED = 3
const val EXTENSIONS_MODIFIED = 4
const val CONTENTS_MODIFIED = 5
const val TEXT_MODIFIED = 6

data class ChapterEvent(val what: Int, val source: Chapter)

const val BOOK_OPENED = 1
const val BOOK_CREATED = 2
const val BOOK_MODIFIED = 3
const val BOOK_CLOSED = 4
const val BOOK_SAVED = 5

data class BookEvent(val what: Int, val source: Book)

object Workbench : CommandHandler {
    var work: Work? = null
        private set

    init {
        Imabw.register(this)
    }

    fun activateWork(work: Work) {
        val last = this.work
        require(work !== last) { "work is already activated" }
        this.work = work.apply {
            path?.let { History.remove(it) }
        }
        last?.apply {
            EventBus.post(BookEvent(BOOK_CLOSED, book))
            path?.let { History.insert(it) }
            cleanup()
        }
        IxIn.actionMap.apply {
            this["saveFile"]?.isDisable = work.path != null
            this["fileDetails"]?.isDisable = work.path == null
        }
    }

    fun ensureSaved(title: String, block: () -> Unit) {
        val work = work!!
        if (!work.isModified) {
            block()
        } else with(confirm(title, tr("d.askSave.hint", work.book.title))) {
            buttonTypes.setAll(ButtonType.CANCEL, ButtonType.YES, ButtonType.NO)
            when (showAndWait().get()) {
                ButtonType.YES -> saveFile { block() }
                ButtonType.NO -> block()
            }
        }
    }

    fun newBook(title: String) {
        activateWork(Work(Book(title)))
        EventBus.post(BookEvent(BOOK_CREATED, work!!.book))
        Imabw.message(tr("d.newBook.success", title))
    }

    fun openBook(param: ParserParam) {
        work?.path?.let {
            require(File(it) != File(param.path)) { "'${param.path} is already opened'" }
        }
        val parser = EpmManager[param.epmName]?.parser
        if (parser == null) {
            error(tr("d.openBook.title"), tr("err.jem.unsupported", param.epmName))
            return
        } else if (parser is FileParser && !File(param.path).exists()) {
            error(tr("d.openBook.title"), tr("err.file.notFound", param.path))
            History.remove(param.path)
            return
        }
        with(LoadBookTask(param)) {
            setOnSucceeded {
                activateWork(Work(value, param))
                EventBus.post(BookEvent(BOOK_OPENED, work!!.book))
                Imabw.message(tr("jem.openBook.success", param.path))
                hideProgress()
            }
            Imabw.submit(this)
        }
    }

    fun saveBook(param: MakerParam, done: (() -> Unit)? = null) {
        val work = work!!
        require(param.book === work.book) { "book to save is not current book" }
        work.inParam?.path?.let {
            if (File(it) == File(param.actualPath)) {
                error(tr("d.saveBook.title"), tr("err.file.opened", param.actualPath))
                return
            }
        }
        if (EpmManager[param.epmName]?.hasMaker != true) {
            error(tr("d.saveBook.title"), tr("err.jem.unsupported", param.epmName))
            return
        }
        with(MakeBookTask(param)) {
            setOnRunning {
                updateProgress(tr("jem.makeBook.hint", param.book.title, param.actualPath.remove(SWAP_SUFFIX)))
            }
            setOnSucceeded {
                work.outParam = param
                EventBus.post(BookEvent(BOOK_SAVED, work.book))
                Imabw.message(tr("jem.saveBook.success", param.book.title, work.path))
                hideProgress()
                done?.invoke()
            }
            Imabw.submit(this)
        }
    }

    fun exportBook(chapter: Chapter) {
        val (file, format) = saveBookFile(chapter.title) ?: return
        work?.path?.let {
            if (File(it) == file) {
                error(tr("d.saveBook.title"), tr("err.file.opened", file))
                return
            }
        }
        with(MakeBookTask(MakerParam(chapter.asBook(), file.path, format, defaultMakerSettings()))) {
            Imabw.submit(this)
        }
    }

    fun exportBooks(chapters: Collection<Chapter>) {
        if (chapters.isEmpty()) return
        if (chapters.size == 1) {
            exportBook(chapters.first())
            return
        }
        val dir = selectDirectory(tr("d.exportBook.title")) ?: return
        val fxApp = Imabw.fxApp.apply { showProgress() }
        val ignored = ArrayList<String>(4)
        val succeed = Vector<String>(chapters.size)
        val failed = Vector<String>(0)
        val opened = work!!.path?.let(::File)
        val counter = AtomicInteger()
        for (chapter in chapters) {
            val path = "${dir.path}/${chapter.title}.$PMAB_NAME"
            if (opened != null && opened == File(path)) {
                counter.incrementAndGet()
                ignored += path
                continue
            }
            val param = MakerParam(chapter.asBook(), path, PMAB_NAME, defaultMakerSettings())
            val task = object : Task<String>() {
                override fun call() = makeBook(param)
            }
            task.setOnRunning {
                fxApp.updateProgress(tr("jem.makeBook.hint", param.book.title, param.actualPath))
            }
            task.setOnSucceeded {
                succeed += param.actualPath
                if (counter.incrementAndGet() == chapters.size) {
                    fxApp.hideProgress()
                    showExportResult(succeed, ignored, failed)
                }
            }
            task.setOnFailed {
                failed += param.actualPath
                Log.d("exportBook", task.exception) { "failed to make book: ${param.path}" }
                if (counter.incrementAndGet() == chapters.size) {
                    fxApp.hideProgress()
                    showExportResult(succeed, ignored, failed)
                }
            }
            Imabw.submit(task)
        }
        if (ignored.size == chapters.size) { // all ignored
            fxApp.hideProgress()
            showExportResult(succeed, ignored, failed)
        }
    }

    internal fun start() {
        println("TODO: parse app arguments: ${App.arguments}")
        newBook(tr("jem.book.untitled"))
    }

    internal fun dispose() {
        work?.apply {
            cleanup()
            path?.let { History.insert(it) }
        }
        History.sync()
    }

    private fun saveFile(done: (() -> Unit)? = null) {
        val work = work!!
        check(work.isModified || work.path == null) { "book is not modified" }
        var outParam = work.outParam
        if (outParam == null) {
            val inParam = work.inParam
            val output = if (inParam?.epmName != PMAB_NAME) {
                saveBookFile(work.book.title, PMAB_NAME)?.first?.path ?: return
            } else { // save pmab to temp file
                (inParam.path + SWAP_SUFFIX).also {
                    Files.setAttribute(Paths.get(it), "dos:hidden", true)
                }
            }
            outParam = MakerParam(work.book, output, PMAB_NAME, defaultMakerSettings())
        }
        saveBook(outParam, done)
    }

    private fun showExportResult(succeed: List<String>, ignored: List<String>, failed: List<String>) {
        with(info(tr("d.exportBook.title"), "")) {
            width = owner.width * 0.5
            dialogPane.content = GridPane().apply {
                hgap = 4.0
                vgap = 4.0
                styleClass += "dialog-content"
                val legend = Label(tr("d.exportBook.result")).apply { styleClass += "form-legend" }
                add(legend, 0, 0, 2, 1)
                init(listOf(
                        Label(tr("d.exportBook.succeed")),
                        Label(tr("d.exportBook.ignored")),
                        Label(tr("d.exportBook.failed"))
                ), listOf(
                        Label(succeed.joinToString("\n") or { tr("misc.empty") }),
                        Label(ignored.joinToString("\n") or { tr("misc.empty") }),
                        Label(failed.joinToString("\n") or { tr("misc.empty") })
                ), 1)
            }
            showAndWait()
        }
    }

    override fun handle(command: String, source: Any): Boolean {
        when (command) {
            "exit" -> ensureSaved(tr("d.exit.title")) { App.exit() }
            "newFile" -> ensureSaved(tr("d.newBook.title")) {
                input(tr("d.newBook.title"), tr("d.newBook.label"), tr("jem.book.untitled"))?.let {
                    newBook(it)
                }
            }
            "openFile" -> ensureSaved(tr("d.openBook.title")) {
                openBookFile()?.let { openBook(ParserParam(it.path)) }
            }
            "saveFile" -> saveFile()
            "saveAsFile" -> exportBook(work!!.book)
            "clearHistory" -> History.clear()
            else -> return false
        }
        return true
    }
}

class Work(val book: Book, val inParam: ParserParam? = null) : EventAction<ChapterEvent> {
    var isModified = false
        internal set(value) {
            field = value
            if (!value) {
                modifications.values.forEach { it.reset() }
                IxIn.actionMap["saveFile"]?.isDisable = true
            } else {
                IxIn.actionMap["saveFile"]?.isDisable = false
            }
        }

    var path = inParam?.path
        private set

    var outParam: MakerParam? = null
        set(value) {
            path = value?.actualPath?.remove(SWAP_SUFFIX)
            field = value
        }

    private val modifications = IdentityHashMap<Chapter, Modification>()

    init {
        EventBus.register(this)
    }

    internal fun cleanup() {
        EventBus.unregistere(this)
        Imabw.submit {
            book.cleanup()
            adjustOutput()
        }
    }

    // rename *.pmab.swp to *.pmab
    private fun adjustOutput() {
        outParam?.actualPath?.takeIf { it.endsWith(SWAP_SUFFIX) }?.let { tmp ->
            Paths.get(tmp.substring(0, tmp.length - SWAP_SUFFIX.length)).apply {
                try {
                    Files.delete(this)
                } catch (e: Exception) {
                    Log.e("Work", e) { "cannot delete temp file '$this'" }
                    return
                }
                try {
                    Files.move(Paths.get(tmp), this)
                } catch (e: Exception) {
                    Log.e("Work", e) { "cannot rename '$tmp' to '$this'" }
                    return
                }
                Files.setAttribute(this, "dos:hidden", false)
            }
        }
    }

    override fun invoke(e: ChapterEvent) {
        val m = modifications.getOrPut(e.source) { Modification() }
        when (e.what) {
            TITLE_MODIFIED -> m.attributes++
            AUTHOR_MODIFIED -> m.attributes++
            ATTRIBUTE_MODIFIED -> m.attributes++
            EXTENSIONS_MODIFIED -> m.extensions++
            CONTENTS_MODIFIED -> m.contents++
            TEXT_MODIFIED -> m.text++
            else -> return
        }
        isModified = true
    }

    private class Modification {
        var text = 0
            set(value) {
                if (value != 0) println("text modified")
                field = value
            }
        var contents = 0
            set(value) {
                if (value != 0) println("contents modified")
                field = value
            }
        var attributes = 0
            set(value) {
                if (value != 0) println("attributes modified")
                field = value
            }
        var extensions = 0
            set(value) {
                if (value != 0) println("extensions modified")
                field = value
            }

        val isModified get() = attributes > 0 || extensions > 0 || contents > 0 || text > 0

        fun reset() {
            text = 0
            contents = 0
            attributes = 0
            extensions = 0
        }
    }
}
