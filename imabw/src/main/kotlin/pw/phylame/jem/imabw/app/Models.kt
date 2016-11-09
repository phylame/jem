/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Imabw.
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

package pw.phylame.jem.imabw.app

import pw.phylame.jem.core.Book
import pw.phylame.jem.core.Chapter
import pw.phylame.jem.epm.EpmManager
import pw.phylame.jem.imabw.app.ui.Dialogs
import pw.phylame.qaf.core.App
import pw.phylame.qaf.core.tr
import pw.phylame.qaf.ixin.Command
import java.io.File
import java.nio.charset.Charset
import java.util.IdentityHashMap
import java.util.LinkedList

class Task(val updater: (Task) -> Unit,
           val book: Book,
           val source: File? = null,
           val format: String = EpmManager.PMAB,
           val arguments: Map<String, Any> = emptyMap()) {

    constructor(updater: (Task) -> Unit, book: Book) : this(updater, book, null)

    // test current book of the task modification state
    val isModified: Boolean get() = isModified(book)

    // test the specified chapter modification state
    fun isModified(chapter: Chapter): Boolean = states[chapter]?.isModified ?: false

    fun textModified(chapter: Chapter, modified: Boolean) {
        val state = getOrNew(chapter)
        state.setTextModified(modified)
        updater(this)
    }

    fun childrenModified(chapter: Chapter, modified: Boolean) {
        val state = getOrNew(chapter)
        state.setChildrenModified(modified)
        updater(this)
    }

    fun attributeModified(chapter: Chapter, modified: Boolean) {
        val state = getOrNew(chapter)
        state.setAttributeModified(modified)
        updater(this)
    }

    fun extensionModified(modified: Boolean) {
        val state = getOrNew(book)
        state.setExtensionModified(modified)
        updater(this)
    }

    fun chapterRemoved(chapter: Chapter) {
        states.remove(chapter)
    }

    fun bookSaved() {
        states.clear()

    }

    fun bookClosed() {
        states.clear()
    }

    // cleanup this task
    fun cleanup() {
        book.cleanup()
    }

    private val states = IdentityHashMap<Chapter, State>()

    private fun getOrNew(chapter: Chapter): State = states.getOrPut(chapter) { State(chapter) }

    // hold modification state of chapter
    private inner class State(val chapter: Chapter) {
        // count of text modification
        var text = 0

        // count of sub-chapter modification
        var children = 0

        // count of attributes modification
        var attribute = 0

        // count of extension(only for book) modification
        var extension = 0

        val isModified: Boolean get() = text > 0 || children > 0 || attribute > 0 || extension > 0

        // reset all to unmodified state
        fun reset() {
            text = 0
            children = 0
            attribute = 0
            extension = 0
        }

        fun setTextModified(modified: Boolean) {
            text += if (modified) 1 else -1
            System.err.println("$chapter text state: $text")
        }

        fun setAttributeModified(modified: Boolean) {
            attribute += if (modified) 1 else -1
            System.err.println("$chapter attribute state: $attribute")
        }

        fun setChildrenModified(modified: Boolean) {
            children += if (modified) 1 else -1
            System.err.println("$chapter children state: $children")
        }

        fun setExtensionModified(modified: Boolean) {
            extension += if (modified) 1 else -1
            System.err.println("$chapter extension state: $extension")
        }

    }
}

object History {
    const val ENCODING = "UTF-8"
    const val FILE_NAME = "history"

    private val histories = LinkedList<String>()
    private var modified = false

    var updater: (() -> Unit)? = null

    private val file: File by lazy {
        File(App.home, "$SETTINGS_DIR$FILE_NAME")
    }

    init {
        load()
        App.cleanups.add(Runnable { sync() })
    }

    val items: Sequence<String> get() = histories.asSequence()

    fun insert(file: File, updating: Boolean = false) {
        if (!AppSettings.historyEnable) {
            return
        }
        val path = file.canonicalPath
        if (path !in histories) {
            if (histories.size > AppSettings.historyLimits) {
                histories.removeLast()
            }
            histories.addFirst(path)
            modified = true
        }
        if (updating) {
            updater?.invoke()
        }
    }

    fun remove(file: File, updating: Boolean = false) {
        if (!AppSettings.historyEnable) {
            return
        }
        val path = file.canonicalPath
        if (histories.remove(path)) {
            modified = true
        }
        if (updating) {
            updater?.invoke()
        }
    }

    fun clear(updating: Boolean = false) {
        if (!AppSettings.historyEnable) {
            return
        }
        if (histories.isNotEmpty()) {
            histories.clear()
            modified = true
        }
        if (updating) {
            updater?.invoke()
        }
    }

    private fun load() {
        if (!AppSettings.historyEnable) {
            return
        }
        val file = file
        if (!file.exists()) {
            return
        }

        val limit = AppSettings.historyLimits

        file.useLines(Charset.forName(ENCODING)) {
            for (line in it) {
                if (line.isEmpty() || line.startsWith('#')) {
                    continue
                }
                if (histories.size > limit) {
                    break
                }
                histories.addLast(line)
            }
        }
    }

    private fun sync() {
        if (!AppSettings.historyEnable) {
            return
        }
        if (!modified) {
            return
        }
        file.writeText(histories.joinToString(System.lineSeparator(), "# DO NOT EDIT THIS FILE"), Charset.forName(ENCODING))
    }
}

object Manager {
    private val _tasks: MutableList<Task> = LinkedList()

    // readonly view of tasks
    val tasks: List<Task> get() = _tasks

    val task: Task? get() = _tasks.firstOrNull()

    fun maybeSaving(title: String): Boolean {
        if (!(task?.isModified ?: false)) { // not modified
            return true
        }
        val option = Dialogs.saving(Imabw.form, title, tr("d.askSaving.tip"))
        return when (option) {
            Dialogs.OPTION_DISCARD -> true
            Dialogs.OPTION_OK -> saveFile()
            else -> false
        }
    }

    @Command(OPEN_FILE)
    fun openFile() {
        openFile(null)
    }

    fun openFile(file: File?) {
        val title = tr("d.openBook.title")
        if (!maybeSaving(title)) {
            return
        }
    }

    @Command(NEW_FILE)
    fun newFile() {
        newFile(null)
    }

    fun newFile(name: String?) {
        val title = tr("d.newBook.title")
        if (!maybeSaving(title)) {
            return
        }
//        val book = worker.newBook(viewer, title, name) ?: return
        task?.cleanup()

//        activateTask(BookTask.fromNewBook(book))
//        app.localizedMessage("d.newBook.finished", book)
    }

    @Command(SAVE_FILE)
    fun saveFile(): Boolean {
        return true
    }

    @Command(SAVE_AS_FILE)
    fun saveAsFile() {
        println(Dialogs.confirm(Imabw.form, "Open File", true, "Let's go\n继续下一步，确定，再确定，确定删除文件么？"))
    }

    @Command(FILE_DETAILS)
    fun viewDetails() {
        val task = task
        if (task != null) {
            Dialogs.bookDetails(Imabw.form, task.book, task.source)
        } else {
            App.error("not found active task")
        }
    }

    @Command(CLEAR_HISTORY)
    fun clearHistory() {
        History.clear(true)
    }

    @Command(EXIT_APP)
    fun exitApp() {
        if (!maybeSaving(tr("d.exitApp.title"))) {
            return
        }
        val task = task
        if (task != null) {
            task.cleanup()
            if (task.source != null) {
                History.insert(task.source, false)
            }
        }
        App.exit(0)
    }
}