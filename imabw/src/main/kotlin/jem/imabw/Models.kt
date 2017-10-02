package jem.imabw

import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.stage.FileChooser
import jem.Book
import jem.epm.EpmManager
import jem.epm.ParserParam
import mala.App
import mala.App.tr
import mala.ixin.Command
import mala.ixin.CommandHandler
import java.io.File

object Workbench : CommandHandler {
    internal val tasks = FXCollections.observableArrayList<Work>()
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
        tasks -= work
    }

    fun ensureSaved(title: String): Boolean {
        return !isModified
    }

    fun newFile(title: String) {
        submit(Work(Book(title)))
    }

    @Command
    fun openFile() {
        val fileChooser = FileChooser()
//        fileChooser.title
        fileChooser.initialDirectory = File("E:/tmp")
        fileChooser.extensionFilters += FileChooser.ExtensionFilter("PMAB File", "*.pmab")
        val file = fileChooser.showOpenDialog(Imabw.form.stage) ?: return
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

    }

    override fun handle(command: String, source: Any): Boolean {
        when (command) {
            "newFile" -> newFile("Book ${tasks.size}")
            "clearHistory" -> History.items.clear()
            else -> return false
        }
        return true
    }
}

class Work(val book: Book, val inParam: ParserParam? = null) {
    val isModified = false

    fun dispose() {}

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
