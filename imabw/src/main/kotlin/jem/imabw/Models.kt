package jem.imabw

import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import jclp.log.Log
import jem.Book
import jem.Chapter
import jem.epm.EpmManager
import jem.epm.MakerParam
import jem.epm.ParserException
import jem.epm.ParserParam
import jem.imabw.toc.NavPane
import jem.imabw.ui.inputText
import jem.imabw.ui.selectBooks
import jem.title
import mala.App
import mala.App.tr
import mala.ixin.Command
import mala.ixin.CommandHandler
import java.util.concurrent.ThreadLocalRandom

object Workbench : CommandHandler {
    private const val TAG = "Workbench"

    val tasks: ObservableList<Work> = FXCollections.observableArrayList<Work>()

    private val isModified get() = tasks.any(Work::isModified)

    init {
        Imabw.register(this)
    }

    fun submit(work: Work) {
        require(work !in tasks) { "Work $work is already submitted" }
        tasks += work.apply {
            path?.let { History.remove(it) }
        }
    }

    fun remove(work: Work) {
        require(work in tasks) { "Work $work is not submitted" }
        tasks -= work.apply {
            dispose()
            path?.let { History.insert(it) }
        }
    }

    fun getTask(book: Book) = tasks.firstOrNull { it.book === book }

    fun isModified(chapter: Chapter) = if (chapter.parent == null) { // root book
        getTask(chapter as Book)?.isModified == true
    } else {
        ThreadLocalRandom.current().nextBoolean()
    }

    fun openBooks(paths: Collection<String>) {
        var expect = 0
        var process = 0
        val end = tasks.size - 1
        for (path in paths) {
            if (tasks.any { it.path == path }) {
                Log.t(TAG) { "'$path' is already opened" }
                continue
            }
            if (expect++ == 0) {
                Imabw.form.beginProgress()
            }
            val param = ParserParam(path)
            val task = object : Task<Book>() {
                override fun call(): Book {
                    return EpmManager.readBook(param) ?: throw ParserException(tr("err.jem.unsupported", param.epmName))
                }
            }
            task.setOnRunning {
                Imabw.form.updateProgress("loading ${param.path}")
            }
            task.setOnSucceeded {
                ++process
                submit(Work(task.value, param))
                if (process + end == expect) {
                    Imabw.form.endProgress()
                    Imabw.message("Opened $process book(s)")
                }
            }
            task.setOnFailed {
                task.exception.printStackTrace()
            }
            task.execute()
        }
    }

    fun exportBook(chapters: Collection<Chapter>) {
        println("save chapters: $chapters")
    }

    fun ensureSaved(title: String): Boolean {
        return !isModified
    }

    fun newFile(title: String) {
        if (title.isEmpty()) {
            inputText(tr("d.newBook.title"), tr("d.newBook.tip"), tr("book.untitled")) {
                submit(Work(Book(it)))
                Imabw.message("Created new book '$it'")
            }
        } else {
            submit(Work(Book(title)))
            Imabw.message("Created new book '$title'")
        }
    }

    @Command
    fun openFile() {
        selectBooks(Imabw.form.stage).map { it.path }.let { openBooks(it) }
    }

    @Command
    fun closeFile() {
        var process = 0
        NavPane.selectBooks.map { getTask(it)!! }.forEach {
            if (it.isModified) {
                // todo ask to save
            }
            remove(it)
            ++process
        }
        if (process > 0) {
            Imabw.message("Closed $process book(s)")
        }
    }

    @Command
    fun exit() {
        if (ensureSaved(tr("d.exit.title"))) {
            App.exit()
        }
    }

    internal fun init() {
        // TODO: parse the app arguments
        newFile(tr("book.untitled"))
    }

    internal fun dispose() {
        tasks.forEach { it.dispose() }
    }

    override fun handle(command: String, source: Any): Boolean {
        when (command) {
            "newFile" -> newFile("")
            "saveAsFile" -> exportBook(NavPane.selectBooks)
            "clearHistory" -> History.clear()
            else -> return false
        }
        return true
    }
}

class Work(val book: Book, val inParam: ParserParam? = null) {
    val isModified = false

    var outParam: MakerParam? = null

    val path get() = outParam?.path ?: inParam?.path

    fun dispose() {
        Imabw.submit {
            println("cleanup book ${book.title}")
            book.cleanup()
        }
    }

    override fun toString(): String {
        return "Work(book=$book, isModified=$isModified)"
    }
}

private class OpenTask(val param: ParserParam) : Task<Book>() {
    init {
        setOnFailed {
            exception.printStackTrace()
        }
    }

    override fun call() = EpmManager.readBook(param)
}

object History {
    private val paths = FXCollections.observableArrayList<String>()

    val emptyBinding: BooleanBinding = Bindings.isEmpty(paths)

    val last get() = paths.firstOrNull()

    init {
        paths.addListener(ListChangeListener {
            val items = Imabw.menuMap["menuHistory"]!!.items
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
                    it.addedSubList.filter { it !in paths }.mapIndexed { i, path ->
                        if (i == 0 && isEmpty) { // insert separator
                            items.add(0, SeparatorMenuItem())
                        }
                        items.add(0, MenuItem(path).apply {
                            setOnAction { Workbench.openBooks(listOf(text)) }
                        })
                    }
                }
            }
        })
    }

    fun remove(path: String) {
        paths.remove(path)
    }

    fun insert(path: String) {
        paths.remove(path)
        paths.add(0, path)
    }

    fun clear() {
        paths.clear()
    }
}
