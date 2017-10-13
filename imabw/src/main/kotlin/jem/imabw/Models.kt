package jem.imabw

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.scene.control.ButtonType
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import jclp.io.createRecursively
import jclp.log.Log
import jem.*
import jem.epm.EpmManager
import jem.epm.MakerParam
import jem.epm.ParserParam
import jem.imabw.ui.*
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
            workProperty.value?.let { old ->
                old.path?.let { History.insert(it) }
                old.cleanup()
            }
            workProperty.value = value.also { new ->
                new.path?.let { History.remove(it) }
            }
            initActions()
        }

    init {
        Imabw.register(this)
    }

    fun ensureSaved(title: String, block: () -> Unit) {
        if (work.isModified) {
            val alert = confirm(title, tr("d.askSave.hint", work.book.title))
            alert.buttonTypes.setAll(ButtonType.CANCEL, ButtonType.YES, ButtonType.NO)
            when (alert.showAndWait().get()) {
                ButtonType.YES -> if (!saveFile()) return
                ButtonType.CANCEL -> return
            }
        }
        block()
    }

    fun newBook(title: String) {
        work = Work(Book(title))
        Imabw.message(tr("jem.newBook.success", title))
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
        with(LoadBookTask(param)) {
            setOnSucceeded {
                work = Work(value, param)
                hideProgress()
                Imabw.message(App.tr("jem.openBook.success", param.path))
            }
            setOnFailed {
                hideProgress()
                App.error("failed to load '${param.path}'", exception)
                History.remove(param.path)
            }
            Imabw.submit(this)
        }
    }

    fun saveBook(param: MakerParam) {
        if (EpmManager[param.epmName]?.hasMaker != true) {
            Log.e(TAG) { "'${param.path}' is unknown for '${param.epmName}'" }
            return
        }
        if (param.path == work.inParam?.path) {
            Log.e(TAG) { "'${param.path}' is currently opened" }
            return
        }
        with(MakeBookTask(param)) {
            setOnSucceeded {
                work.outParam = param
                work.isModified = false
                work.path = value.replace(SWAP_SUFFIX, "")
                Imabw.message(tr("jem.saveBook.success", param.book.title, work.path))
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
            val settings = defaultMakerSettings()
            param = if (inParam?.epmName == "pmab") { // save pmab to temp file
                MakerParam(work.book, inParam.path + SWAP_SUFFIX, "pmab", settings)
            } else {
                MakerParam(work.book, saveBookFile(work.book.title)?.path ?: return false, "pmab", settings)
            }
        }
        saveBook(param)
        return true
    }

    fun exportFile(chapter: Chapter) {
        val file = saveBookFile(chapter.title) ?: return
        val param = MakerParam(chapter.asBook(), file.path, arguments = defaultMakerSettings())
        Imabw.submit(MakeBookTask(param))
    }

    fun exportFiles(chapters: Collection<Chapter>) {
        if (chapters.isEmpty()) return
        if (chapters.size == 1) {
            exportFile(chapters.first())
        } else {
            val dir = selectDirectory(tr("d.exportBook.title"), Imabw.fxApp.stage) ?: return
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
            outParam?.path?.takeIf { it.endsWith(SWAP_SUFFIX) }?.let {
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
