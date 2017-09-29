package jem.imabw

import javafx.collections.FXCollections
import jem.Book
import jem.Chapter

object Workbench {
    internal val tasks = FXCollections.observableArrayList<Task>()
    val isModified get() = tasks.any(Task::isModified)

    fun submit(task: Task) {
        require(task !in tasks) { "Task $task is already submitted" }
        tasks += task
    }

    fun remove(task: Task) {
        require(task in tasks) { "Task $task is not submitted" }
        tasks -= task
    }

    internal fun init() {
        submit(Task(Book("Example").also {
            for (i in 1..18) {
                it.append(Chapter("Chapter $i").also {
                    for (j in 1..4) {
                        it.append(Chapter("Chapter $i.$j"))
                    }
                })
            }
        }))
        submit(Task(Book("Example")))
        submit(Task(Book("Example").also {
            for (i in 1..18) {
                it.append(Chapter("Chapter $i"))
            }
        }))
        submit(Task(Book("Example")))
    }

    internal fun dispose() {

    }
}

class Task(val book: Book) {
    val isModified = false

    fun dispose() {}

    override fun toString(): String {
        return "Task(book=$book, isModified=$isModified)"
    }
}
