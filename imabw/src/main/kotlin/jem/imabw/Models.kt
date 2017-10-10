package jem.imabw

import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.concurrent.Task
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import jem.Book
import jem.Chapter
import jem.epm.EpmManager
import jem.epm.MakerParam
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
    val tasks = FXCollections.observableArrayList<Work>()

    val isModified get() = tasks.any(Work::isModified)

    init {
        Imabw.register(this)
    }

    fun submit(work: Work) {
        require(work !in tasks) { "Work $work is already submitted" }
        tasks += work.apply {
            this.path?.let { History.remove(it) }
        }
    }

    fun remove(work: Work) {
        require(work in tasks) { "Work $work is not submitted" }
        tasks -= work.apply {
            dispose()
            this.path?.let { History.insert(it) }
        }
    }

    fun getTask(book: Book) = tasks.firstOrNull { it.book === book }

    fun isModified(chapter: Chapter): Boolean {
        return if (chapter.parent == null) { // root book
            getTask(chapter as Book)?.isModified == true
        } else {
            ThreadLocalRandom.current().nextBoolean()
        }
    }

    fun openBook(paths: Collection<String>) {
        for (path in paths) {
            if (tasks.any { it.path == path }) {
                println("'$path' is already opened")
                continue
            }
            val task = OpenTask(ParserParam(path))
            task.setOnSucceeded {
                submit(Work(task.value, task.param))
            }
            Imabw.submit(task)
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
            }
        } else {
            submit(Work(Book(title)))
        }
    }

    @Command
    fun openFile() {
        selectBooks(Imabw.form.stage).map { it.path }.let { openBook(it) }
    }

    @Command
    fun closeFile() {
        NavPane.selectBooks.map { getTask(it)!! }.forEach {
            if (it.isModified) {
                // todo ask to save
            }
            remove(it)
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
        Imabw.message(App.tr("status.ready"))
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
        println("cleanup book ${book.title}")
        book.cleanup()
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
                            setOnAction { Workbench.openBook(listOf(text)) }
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
