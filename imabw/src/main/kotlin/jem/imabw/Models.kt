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
import javafx.geometry.VPos
import javafx.scene.control.Alert
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
import jem.imabw.ui.EditorPane
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

enum class ModificationType {
    ATTRIBUTE_MODIFIED,
    EXTENSIONS_MODIFIED,
    CONTENTS_MODIFIED,
    TEXT_MODIFIED
}

data class ModificationEvent(val chapter: Chapter, val what: ModificationType, val count: Int = 1)

enum class WorkflowType {
    BOOK_OPENED,
    BOOK_CREATED,
    BOOK_MODIFIED,
    BOOK_CLOSED,
    BOOK_SAVED
}

data class WorkflowEvent(val book: Book, val what: WorkflowType)

object Workbench : CommandHandler {
    var work: Work? = null
        private set

    init {
        History // init history
        Imabw.register(this)
    }

    fun activateWork(work: Work) {
        val last = this.work
        require(work !== last) { "work is already activated" }
        this.work = work.apply {
            path?.let { History.remove(it) }
        }
        last?.apply {
            path?.let { History.insert(it) }
            EventBus.post(WorkflowEvent(book, WorkflowType.BOOK_CLOSED))
            cleanup()
        }
        IxIn.actionMap.apply {
            this["saveFile"]?.isDisable = work.path != null
            this["fileDetails"]?.isDisable = work.path == null
        }
    }

    fun ensureSaved(title: String, block: () -> Unit) {
        val work = work
        if (work == null) {
            block()
        } else if (!work.isModified) {
            block()
        } else with(alert(Alert.AlertType.CONFIRMATION, title, tr("d.askSave.hint", work.book.title))) {
            buttonTypes.setAll(ButtonType.CANCEL, ButtonType.YES, ButtonType.NO)
            when (showAndWait().get()) {
                ButtonType.YES -> saveFile(block)
                ButtonType.NO -> block()
            }
        }
    }

    fun newBook(title: String) {
        with(Imabw.fxApp) {
            showProgress()
            activateWork(Work(Book(title)))
            EventBus.post(WorkflowEvent(work!!.book, WorkflowType.BOOK_CREATED))
            Imabw.message(tr("d.newBook.success", title))
            hideProgress()
        }
    }

    fun openBook(param: ParserParam) {
        work?.path?.let {
            if (Paths.get(it) == Paths.get(param.path)) {
                Log.w("openBook") { "'${param.path} is already opened'" }
                return
            }
        }
        val parser = EpmManager[param.epmName]?.parser
        if (parser != null) {
            if (parser is FileParser && !File(param.path).exists()) {
                error(tr("d.openBook.title"), tr("err.file.notFound", param.path))
                History.remove(param.path)
                return
            }
        } else {
            error(tr("d.openBook.title"), tr("err.jem.unsupported", param.epmName))
            return
        }
        with(LoadBookTask(param)) {
            setOnSucceeded {
                activateWork(Work(value, param))
                EventBus.post(WorkflowEvent(work!!.book, WorkflowType.BOOK_OPENED))
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
        val task = object : MakeBookTask(param) {
            override fun call(): String {
                EditorPane.cacheTabs()
                return makeBook(this.param)
            }
        }
        task.setOnRunning {
            task.updateProgress(tr("jem.makeBook.hint", param.book.title, param.actualPath.remove(SWAP_SUFFIX)))
        }
        task.setOnSucceeded {
            work.outParam = param
            work.resetModifications()
            IxIn.actionMap["saveFile"]?.isDisable = true
            IxIn.actionMap["fileDetails"]?.isDisable = false
            EventBus.post(WorkflowEvent(work.book, WorkflowType.BOOK_SAVED))
            Imabw.message(tr("jem.saveBook.success", param.book.title, work.path))
            task.hideProgress()
            done?.invoke()
        }
        Imabw.submit(task)
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
        val path = App.arguments.firstOrNull()?.let { Paths.get(it) }
        if (path != null && Files.isRegularFile(path)) {
            openFile(path.toString())
        } else {
            newBook(tr("jem.book.untitled"))
        }
    }

    internal fun dispose() {
        work?.apply {
            path?.let { History.insert(it) }
            cleanup()
        }
        History.sync()
    }

    internal fun openFile(path: String) {
        ensureSaved(tr("d.openBook.title")) {
            if (path.isEmpty()) {
                openBookFile()?.let { openBook(ParserParam(it.path)) }
            } else {
                val file = Paths.get(path).toAbsolutePath()
                openBook(ParserParam(if (Files.exists(file)) file.normalize().toString() else path))
            }
        }
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
                inParam.path + SWAP_SUFFIX
            }
            outParam = MakerParam(work.book, output, PMAB_NAME, defaultMakerSettings())
        }
        saveBook(outParam, done)
    }

    private fun showExportResult(succeed: List<String>, ignored: List<String>, failed: List<String>) {
        with(info(tr("d.exportBook.title"), "")) {
            width = owner.width * 0.5
            dialogPane.content = GridPane().apply {
                val legend = Label(tr("d.exportBook.result")).apply {
                    style = "-fx-font-weight: bold;"
                }
                add(legend, 0, 0, 2, 1)
                init(listOf(
                        Label(tr("d.exportBook.succeed")),
                        Label(tr("d.exportBook.ignored")),
                        Label(tr("d.exportBook.failed"))
                ), listOf(
                        Label(succeed.joinToString("\n") or { tr("misc.empty") }),
                        Label(ignored.joinToString("\n") or { tr("misc.empty") }),
                        Label(failed.joinToString("\n") or { tr("misc.empty") })
                ), 1, VPos.TOP)
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
            "openFile" -> openFile("")
            "saveFile" -> saveFile()
            "saveAsFile" -> exportBook(work!!.book)
            "fileDetails"->{
                println(work?.book?.extensions?.get(EXT_EPM_FILE_INFO))
            }
            "clearHistory" -> History.clear()
            else -> return false
        }
        return true
    }
}

class Work(val book: Book, val inParam: ParserParam? = null) : EventAction<ModificationEvent> {
    val isModified get() = modifications.values.any { it.isModified }

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

    internal fun resetModifications() {
        modifications.values.forEach { it.reset() }
    }

    // rename *.pmab.swp to *.pmab
    private fun adjustOutput() {
        outParam?.actualPath?.takeIf { it.endsWith(SWAP_SUFFIX) }?.let { tmp ->
            val swap = File(tmp)
            if (!swap.exists()) {
                Log.t("Work") { "no swap file found" }
                return
            }
            File(tmp.substring(0, tmp.length - SWAP_SUFFIX.length)).apply {
                if (!delete() || !swap.renameTo(this)) {
                    Log.e("Work") { "cannot rename '$tmp' to '$this'" }
                }
            }
        }
    }

    private fun notifyModified() {
        IxIn.actionMap["saveFile"]?.isDisable = !isModified
        EventBus.post(WorkflowEvent(book, WorkflowType.BOOK_MODIFIED))
    }

    override fun invoke(e: ModificationEvent) {
        val m = modifications.getOrPut(e.chapter) { Modification() }
        when (e.what) {
            ModificationType.ATTRIBUTE_MODIFIED -> m.attributes += e.count
            ModificationType.EXTENSIONS_MODIFIED -> m.extensions += e.count
            ModificationType.CONTENTS_MODIFIED -> m.contents += e.count
            ModificationType.TEXT_MODIFIED -> m.text += e.count
        }
        notifyModified()
    }

    private class Modification {
        var text = 0

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
