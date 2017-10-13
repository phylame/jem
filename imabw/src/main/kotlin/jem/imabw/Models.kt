package jem.imabw

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.concurrent.Task
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import jclp.io.createRecursively
import jclp.log.Log
import jem.Book
import jem.Chapter
import jem.asBook
import jem.epm.EpmManager
import jem.epm.MakerParam
import jem.epm.ParserParam
import jem.imabw.ui.inputText
import jem.imabw.ui.openBookFile
import jem.imabw.ui.saveBookFile
import jem.imabw.ui.selectDirectory
import jem.title
import mala.App
import mala.App.tr
import mala.ixin.Command
import mala.ixin.CommandHandler
import mala.ixin.IxIn
import java.io.File

object Workbench : CommandHandler {
    private const val TAG = "Workbench"

    val workProperty = SimpleObjectProperty<Work>()

    var work: Work
        get() = workProperty.value
        set(value) {
            workProperty.value?.let {
                it.path?.let { History.insert(it) }
                it.cleanup()
            }
            workProperty.value = value.also {
                it.path?.let { History.remove(it) }
            }
            initActions()
        }

    init {
        Imabw.register(this)
    }

    inline fun ensureSaved(title: String, block: () -> Unit) {
        if (work.isModified) {
            println("ask save for $title")
//            saveFile()
        }
        block()
    }

    fun newBook(title: String) {
        work = Work(Book(title))
        Imabw.message("Created new book '$title'")
    }

    fun openBook(param: ParserParam) {
        if (work.path == param.path) {
            Log.t(TAG) { "'${param.path}' is already opened" }
            return
        }
        if (EpmManager[param.epmName]?.hasParser != true) {
            Log.e(TAG) { "'${param.path}' is unknown for '${param.epmName}'" }
            return
        }
        val task = LoadBookTask(param)
        task.setOnSucceeded {
            work = Work(task.value, param)
            Imabw.message("Opened book '${param.path}'")
        }
        task.setOnFailed {
            println("show exception dialog")
            task.exception.printStackTrace()
            History.remove(param.path)
        }
        task.execute()
    }

    fun saveBook(param: MakerParam) {
        if (EpmManager[param.epmName]?.hasMaker != true) {
            Log.e(TAG) { "'${param.path}' is unknown for '${param.epmName}'" }
            return
        }
        with(SaveBookTask(param)) {
            setOnSucceeded {
                work.path = value
                work.outParam = param
                work.isModified = false
            }
            setOnFailed {
                exception.printStackTrace()
            }
            execute()
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
    fun saveFile() {
        if (!work.isModified && work.path != null) {
            Log.d(TAG) { "Book is not modified" }
            return
        }
        var param = work.outParam
        if (param == null) {
            val file = saveBookFile(Imabw.fxApp.stage, work.book.title) ?: return
            param = MakerParam(work.book, file.path, "pmab", defaultMakerSettings())
        }
        saveBook(param)
    }

    fun exportFile(chapter: Chapter) {
        val file = saveBookFile(Imabw.fxApp.stage, chapter.title) ?: return
        val param = MakerParam(chapter.asBook(), file.path, arguments = defaultMakerSettings())
        with(SaveBookTask(param)) {
            setOnSucceeded {

            }
            setOnFailed {
                exception.printStackTrace()
            }
            execute()
        }
    }

    fun exportFiles(chapters: Collection<Chapter>) {
        if (chapters.isEmpty()) return
        if (chapters.size == 1) {
            exportFile(chapters.first())
        } else {
            val dir = selectDirectory(Imabw.fxApp.stage, tr("d.exportBook.title")) ?: return
            val params = chapters.map { MakerParam(it.asBook(), dir.path, "pmab", defaultMakerSettings()) }
//            ExportBookTask(params) { Imabw.message("Saved '${params.size}' book(s)") }.execute()
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
                openBookFile(Imabw.fxApp.stage)?.let { openBook(ParserParam(it.path)) }
            }
            "saveAsFile" -> exportFile(work.book)
            "clearHistory" -> History.clear()
            else -> return false
        }
        return true
    }
}

class Work(val book: Book, val inParam: ParserParam? = null) {
    val modifiedProperty = SimpleBooleanProperty()

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
            println("cleanup book ${book.title}")
            book.cleanup()
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
        }
    }

    fun insert(path: String) {
        if (GeneralSettings.enableHistory) {
            paths.remove(path)
            if (paths.size == GeneralSettings.historyLimit) {
                paths.remove(paths.size - 1, paths.size)
            }
            paths.add(0, path)
        }
    }

    fun clear() {
        if (GeneralSettings.enableHistory) {
            paths.clear()
        }
    }

    fun load() {
        if (GeneralSettings.enableHistory) {
            if (file.exists()) {
                val task = object : Task<List<String>>() {
                    override fun call() = file.reader().readLines()
                }
                task.setOnSucceeded {
                    paths += task.value
                }
                task.setOnFailed {
                    task.exception.printStackTrace()
                }
                task.execute()
            }
        }
    }

    fun sync() {
        if (GeneralSettings.enableHistory && paths.isNotEmpty()) {
            if (file.exists() || file.parentFile.createRecursively()) {
                file.bufferedWriter().use { out ->
                    paths.forEach { out.append(it).append("\n") }
                }
            }
        }
    }
}
