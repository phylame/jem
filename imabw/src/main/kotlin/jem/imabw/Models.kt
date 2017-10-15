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

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.concurrent.Task
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.layout.GridPane
import jclp.EventBus
import jclp.io.createRecursively
import jclp.log.Log
import jclp.text.or
import jem.*
import jem.epm.*
import jem.imabw.ui.*
import mala.App
import mala.App.tr
import mala.ixin.Command
import mala.ixin.CommandHandler
import mala.ixin.IxIn
import mala.ixin.init
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

private const val SWAP_SUFFIX = ".swp"

enum class BookEvent {
    CREATED, OPENED, SAVED
}

object Workbench : CommandHandler {
    private const val TAG = "Workbench"

    val workProperty = SimpleObjectProperty<Work>()

    var work: Work
        get() = workProperty.value
        set(value) {
            val old = workProperty.value
            workProperty.value = value.also { new ->
                new.path?.let { History.remove(it) }
            }
            old?.apply {
                path?.let { History.insert(it) }
                cleanup()
            }
            initActions()
        }

    init {
        Imabw.register(this)
    }

    fun ensureSaved(title: String, block: () -> Unit) {
        if (work.isModified) {
            with(confirm(title, tr("d.askSave.hint", work.book.title))) {
                buttonTypes.setAll(ButtonType.CANCEL, ButtonType.YES, ButtonType.NO)
                when (showAndWait().get()) {
                    ButtonType.YES -> if (!saveFile()) return
                    ButtonType.CANCEL -> return
                }
            }
        }
        block()
    }

    fun newBook(title: String) {
        work = Work(Book(title))
        EventBus.post(BookEvent.CREATED)
        Imabw.message(tr("d.newBook.success", title))
    }

    fun openBook(param: ParserParam) {
        if (work.path == param.path) {
            Log.t(TAG) { "'${param.path}' is already opened" }
            return
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
                work = Work(value, param)
                Imabw.message(App.tr("jem.openBook.success", param.path))
                EventBus.post(BookEvent.OPENED)
                hideProgress()
            }
            Imabw.submit(this)
        }
    }

    fun saveBook(param: MakerParam) {
        if (EpmManager[param.epmName]?.hasMaker != true) {
            error(tr("d.saveBook.title"), tr("err.jem.unsupported", param.epmName))
            return
        }
        work.inParam?.path?.let { path ->
            if (File(path) == File(param.actualPath)) {
                error(tr("d.saveBook.title"), tr("err.file.opened", param.actualPath))
                return
            }
        }
        with(MakeBookTask(param)) {
            setOnRunning {
                updateProgress(App.tr("jem.makeBook.hint", param.book.title, param.actualPath.replace(SWAP_SUFFIX, "")))
            }
            setOnSucceeded {
                work.outParam = param
                work.isModified = false
                work.path = value.replace(SWAP_SUFFIX, "")
                Imabw.message(tr("jem.saveBook.success", param.book.title, work.path))
                EventBus.post(BookEvent.SAVED)
                hideProgress()
            }
            Imabw.submit(this)
        }
    }

    internal fun start() {
        // TODO: parse the app arguments
        newBook(tr("book.untitled"))
    }

    internal fun dispose() {
        work.cleanup()
        work.path?.let { History.insert(it) }
        History.sync()
    }

    private fun initActions() {
        val work = work
        val actionMap = IxIn.actionMap
        actionMap["clearHistory"]?.disableProperty?.bind(Bindings.isEmpty(History.paths))
        actionMap["saveFile"]?.disableProperty?.bind(work.modifiedProperty.not().and(work.pathProperty.isNotNull))
        actionMap["fileDetails"]?.disableProperty?.bind(work.pathProperty.isNull)
    }

    @Command
    fun saveFile(): Boolean {
        if (!work.isModified && work.path != null) {
            Log.d(TAG) { "book is not modified" }
            return false
        }
        var param = work.outParam
        if (param == null) {
            val inParam = work.inParam
            val output = if (inParam?.epmName != PMAB_NAME) {
                saveBookFile(work.book.title, PMAB_NAME)?.first?.path ?: return false
            } else { // save pmab to temp file
                inParam.path + SWAP_SUFFIX
            }
            param = MakerParam(work.book, output, PMAB_NAME, defaultMakerSettings())
        }
        saveBook(param)
        return true
    }

    fun exportFile(chapter: Chapter) {
        val (file, format) = saveBookFile(chapter.title) ?: return
        work.path?.let { path ->
            if (File(path) == file) {
                error(tr("d.saveBook.title"), tr("err.file.opened", file))
                return
            }
        }
        with(MakeBookTask(MakerParam(chapter.asBook(), file.path, format, defaultMakerSettings()))) {
            Imabw.submit(this)
        }
    }

    fun exportFiles(chapters: Collection<Chapter>) {
        if (chapters.isEmpty()) return
        if (chapters.size == 1) {
            exportFile(chapters.first())
            return
        }
        val dir = selectDirectory(tr("d.exportBook.title")) ?: return
        val ignored = ArrayList<String>(4)
        val succeed = Vector<String>(chapters.size)
        val failed = Vector<String>(0)
        val file = work.path?.let(::File)
        val counter = AtomicInteger()
        val fxApp = Imabw.fxApp
        fxApp.showProgress()
        for (chapter in chapters) {
            val path = "${dir.path}/${chapter.title}.$PMAB_NAME"
            if (file != null && file == File(path)) {
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
            "exit" -> ensureSaved(tr("d.exit.title")) {
                App.exit()
            }
            "newFile" -> ensureSaved(tr("d.newBook.title")) {
                inputText(tr("d.newBook.title"), tr("d.newBook.tip"), tr("book.untitled"))?.let {
                    newBook(it)
                }
            }
            "openFile" -> ensureSaved(tr("d.openBook.title")) {
                openBookFile()?.let { openBook(ParserParam(it.path)) }
            }
            "saveAsFile" -> exportFile(work.book)
            "clearHistory" -> History.clear()
            else -> return false
        }
        return true
    }
}

class Work(val book: Book, val inParam: ParserParam? = null) {
    private val TAG = "Work"

    val modifiedProperty = SimpleBooleanProperty()

    val titleProperty = SimpleStringProperty(book.title)

    val authorProperty = SimpleStringProperty(book.author)

    val pathProperty = SimpleStringProperty(inParam?.path)

    var isModified: Boolean
        get() = modifiedProperty.value
        set(value) {
            modifiedProperty.value = value
        }

    var path: String?
        get() = pathProperty.value
        set(value) {
            pathProperty.value = value
        }

    var outParam: MakerParam? = null

    fun cleanup() {
        Imabw.submit {
            book.cleanup()
            outParam?.actualPath?.takeIf { it.endsWith(SWAP_SUFFIX) }?.let {
                File(it.substring(0, it.length - 4)).apply {
                    if (delete()) {
                        if (!File(it).renameTo(this)) {
                            Log.e(TAG) { "cannot rename '$it' to '$this'" }
                        }
                    } else {
                        Log.e(TAG) { "cannot delete temp file '$this'" }
                    }
                }
            }
        }
    }
}

private class Modification {
    val attributes = SimpleIntegerProperty()
    val extensions = SimpleIntegerProperty()
    val contents = SimpleIntegerProperty()
    val text = SimpleIntegerProperty()

    val modified = attributes.greaterThan(0)
            .or(extensions.greaterThan(0))
            .or(contents.greaterThan(0))
            .or(text.greaterThan(0))
}

object History {
    private val file = File(App.home, "config/history.txt")

    internal val paths = FXCollections.observableArrayList<String>()

    val latest get() = paths.firstOrNull()

    private var isModified = false

    init {
        paths.addListener(ListChangeListener {
            val items = IxIn.menuMap["menuHistory"]!!.items
            val isEmpty = items.size == 1
            while (it.next()) {
                if (it.wasRemoved()) {
                    for (path in it.removed) {
                        items.removeIf { it.text == path }
                    }
                    if (items.size == 2) { // remove separator
                        items.remove(0, 1)
                    }
                }
                if (it.wasAdded()) {
                    val paths = items.map { it.text }
                    it.addedSubList.filter { it !in paths }.asReversed().mapIndexed { i, path ->
                        if (i == 0 && isEmpty) { // insert separator
                            items.add(0, SeparatorMenuItem())
                        }
                        items.add(0, MenuItem(path).apply {
                            setOnAction { Workbench.openBook(ParserParam(text)) }
                        })
                    }
                }
            }
        })
        load()
    }

    fun remove(path: String) {
        if (GeneralSettings.enableHistory) {
            paths.remove(path)
            isModified = true
        }
    }

    fun insert(path: String) {
        if (GeneralSettings.enableHistory) {
            paths.remove(path)
            if (paths.size == GeneralSettings.historyLimit) {
                paths.remove(paths.size - 1, paths.size)
            }
            paths.add(0, path)
            isModified = true
        }
    }

    fun clear() {
        if (GeneralSettings.enableHistory) {
            paths.clear()
            isModified = true
        }
    }

    fun load() {
        if (GeneralSettings.enableHistory) {
            if (file.exists()) {
                with(ReadLineTask(file)) {
                    setOnSucceeded {
                        paths += value
                        hideProgress()
                    }
                    Imabw.submit(this)
                }
            }
        }
    }

    fun sync() {
        if (GeneralSettings.enableHistory && isModified) {
            if (file.exists() || file.parentFile.createRecursively()) {
                file.bufferedWriter().use { out ->
                    paths.forEach { out.append(it).append("\n") }
                }
            }
        }
    }
}
