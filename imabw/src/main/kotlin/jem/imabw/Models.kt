package jem.imabw

import javafx.collections.FXCollections
import javafx.concurrent.Task
import jem.Book
import jem.epm.EpmManager
import jem.epm.ParserParam
import mala.App
import mala.App.tr
import mala.ixin.Command
import mala.ixin.CommandHandler

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
//        val fileChooser = FileChooser()
//        val file = fileChooser.showOpenDialog(Imabw.form.stage)
//        println(file)
        val path = "d:/downloads/xz.pmab"
        val task = OpenTask(ParserParam(path))
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
        newFile(tr("book.untitled"))
        Imabw.message(App.tr("status.ready"))
    }

    internal fun dispose() {

    }

    override fun handle(command: String, source: Any): Boolean {
        when (command) {
            "newFile" -> newFile("Example ${tasks.size}")
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
