package jem.imabw

import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.stage.WindowEvent
import jclp.text.textOf
import jem.Book
import jem.Chapter
import jem.epm.parseBook
import jem.intro
import mala.App

object Workbench {
    internal val tasks = FXCollections.observableArrayList<Work>()
    val isModified get() = tasks.any(Work::isModified)

    fun submit(work: Work) {
        require(work !in tasks) { "Work $work is already submitted" }
        tasks += work
    }

    fun remove(work: Work) {
        require(work in tasks) { "Work $work is not submitted" }
        tasks -= work
    }

    fun requestQuit(event: WindowEvent? = null) {
        if (isModified) {
            event?.consume()
            return
        }
        App.exit(0)
    }

    internal fun init() {
        submit(Work(Book("Example").also {
            for (i in 1..18) {
                it.append(Chapter("Chapter $i").also {
                    it.intro = textOf("Here are some intro for this chapter\nThat's next line")
                    for (j in 1..4) {
                        it.append(Chapter("Chapter $i.$j"))
                    }
                })
            }
        }))
        submit(Work(Book("Example")))
        submit(Work(Book("Example").also {
            for (i in 1..18) {
                it.append(Chapter("Chapter $i"))
            }
        }))
        submit(Work(Book("Example")))
        val task = object : Task<Book?>() {
            override fun call(): Book? {
                return parseBook("E:/tmp/2", "pmab")
            }
        }
        task.setOnSucceeded {
            submit(Work(task.value!!))
        }
        Imabw.submit(task)
        Imabw.print(App.tr("status.ready"))
    }

    internal fun dispose() {

    }
}

class Work(val book: Book) {
    val isModified = false

    fun dispose() {}

    override fun toString(): String {
        return "Work(book=$book, isModified=$isModified)"
    }
}
