package jem.imabw

import javafx.collections.FXCollections
import javafx.concurrent.Task
import jem.Book
import jem.Chapter
import jem.epm.EpmManager
import jem.epm.ParserParam
import jem.imabw.toc.NavPane
import jem.imabw.ui.openBook
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
        tasks += work
    }

    fun remove(work: Work) {
        require(work in tasks) { "Work $work is not submitted" }
        tasks -= work.apply { dispose() }
    }

    fun getTask(book: Book) = tasks.firstOrNull { it.book === book }

    fun isModified(chapter: Chapter): Boolean {
        return if (chapter.parent == null) { // root book
            getTask(chapter as Book)?.isModified == true
        } else
            ThreadLocalRandom.current().nextBoolean()
    }

    fun exportBook(chapters: Collection<Chapter>) {
        println("save chapters: $chapters")
    }

    fun ensureSaved(title: String): Boolean {
        return !isModified
    }

    fun newFile(title: String) {
        submit(Work(Book(title)))
    }

    @Command
    fun openFile() {
        openBook(Imabw.form.stage).forEach {
            if (tasks.first())
        }

        val task = OpenTask(ParserParam(file.path))
        task.setOnSucceeded {
            submit(Work(task.value, task.param))
        }
        task.setOnFailed {
            task.exception.printStackTrace()
        }
        Imabw.submit(task)
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
            "newFile" -> newFile("Book ${tasks.size}")
            "saveAsFile" -> exportBook(NavPane.selectBooks)
            "clearHistory" -> History.items.clear()
            else -> return false
        }
        return true
    }
}

class Work(val book: Book, val inParam: ParserParam? = null) {
    val isModified = false

    fun dispose() {
        println("cleanup book ${book.title}")
        book.cleanup()
    }

    override fun toString(): String {
        return "Work(book=$book, isModified=$isModified)"
    }
}

private class OpenTask(val param: ParserParam) : Task<Book>() {
    override fun call() = EpmManager.readBook(param)
}

object History {
    val items = FXCollections.observableArrayList<String>()
}
