/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

package jem.imabw.app

import jem.imabw.app.ui.Dialogs
import pw.phylame.jem.core.Book
import pw.phylame.jem.core.Chapter
import pw.phylame.jem.epm.EpmManager
import pw.phylame.qaf.core.App
import pw.phylame.qaf.core.tr
import pw.phylame.qaf.ixin.Command
import rx.Observer
import java.io.File
import java.nio.charset.Charset
import java.util.*

class Task(val book: Book,
           val updater: (Task) -> Unit,
           val source: File? = null,
           val format: String = EpmManager.PMAB,
           val arguments: Map<String, Any> = emptyMap()) {

    constructor(book: Book, updater: (Task) -> Unit) : this(book, updater, null)

    // test current book of the activeTask modification state
    val isModified: Boolean get() = isModified(book)

    // test the specified chapter modification state
    fun isModified(chapter: Chapter): Boolean = states[chapter]?.isModified ?: false

    fun textModified(chapter: Chapter, modified: Boolean) {
        val state = stateOf(chapter)
        state.textModified(modified)
        updater(this)
    }

    fun contentsModified(chapter: Chapter, modified: Boolean) {
        val state = stateOf(chapter)
        state.contentsModified(modified)
        updater(this)
    }

    fun attributeModified(chapter: Chapter, modified: Boolean) {
        val state = stateOf(chapter)
        state.attributeModified(modified)
        updater(this)
    }

    fun extensionModified(modified: Boolean) {
        val state = stateOf(book)
        state.extensionModified(modified)
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

    fun cleanup() {
        book.cleanup()
    }

    private val states = IdentityHashMap<Chapter, State>()

    private fun stateOf(chapter: Chapter): State = states.getOrPut(chapter) { State(chapter) }

    // hold modification state of chapter
    private inner class State(val chapter: Chapter) {
        // count of text modification
        var text = 0

        // count of contents modification
        var contents = 0

        // count of attributes modification
        var attribute = 0

        // count of extension(only for book) modification
        var extension = 0

        val isModified: Boolean get() = text > 0 || contents > 0 || attribute > 0 || extension > 0

        // reset all to unmodified state
        fun reset() {
            text = 0
            contents = 0
            attribute = 0
            extension = 0
        }

        fun textModified(modified: Boolean) {
            text += if (modified) 1 else -1
            if (text < 0) {
                text = 0
            }
            App.echo("$chapter text state: $text")
        }

        fun attributeModified(modified: Boolean) {
            attribute += if (modified) 1 else -1
            if (attribute < 0) {
                attribute = 0
            }
            App.echo("$chapter attribute state: $attribute")
        }

        fun contentsModified(modified: Boolean) {
            contents += if (modified) 1 else -1
            if (contents < 0) {
                contents = 0
            }
            App.echo("$chapter contents state: $contents")
        }

        fun extensionModified(modified: Boolean) {
            extension += if (modified) 1 else -1
            if (extension < 0) {
                extension = 0
            }
            App.echo("$chapter extension state: $extension")
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

    fun append(file: File, updating: Boolean = false) {
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
            if (updating) {
                updater?.invoke()
            }
        }
    }

    fun remove(file: File, updating: Boolean = false) {
        if (!AppSettings.historyEnable) {
            return
        }
        val path = file.canonicalPath
        if (histories.remove(path)) {
            modified = true
            if (updating) {
                updater?.invoke()
            }
        }
    }

    fun clear(updating: Boolean = false) {
        if (!AppSettings.historyEnable) {
            return
        }
        if (histories.isNotEmpty()) {
            histories.clear()
            modified = true
            if (updating) {
                updater?.invoke()
            }
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
    var task: Task? = null
        set (value) {
            if (value == null) {
                throw NullPointerException("task cannot be null")
            }
            if (value === field) { // already activated
                return
            }
            val form = Imabw.form
            var source = field?.source
            field = value
            form.updateBook(value.book)
            if (source != null) {
                History.append(source, false)
            }
            source = value.source
            Dialogs.setCurrentDirectory(value.source)
            if (source != null) {
                History.remove(source, true)
                form.actions[FILE_DETAILS]?.isEnabled = true
            } else {
                form.actions[FILE_DETAILS]?.isEnabled = false
            }
        }

    fun maybeSaving(title: String): Boolean {
        if (!(task?.isModified ?: false)) { // not modified
            return true
        }
        return when (Dialogs.saving(Imabw.form, title, tr("d.askSaving.tip"))) {
            Dialogs.OPTION_DISCARD -> true
            Dialogs.OPTION_OK -> saveFile()
            else -> false
        }
    }

    fun openBook(title: String, param: EpmInParam) {
        if (!param.file.exists()) {
            Dialogs.error(Imabw.form, title, tr("d.openBook.fileNotExist", param.file))
        }
        if (!EpmManager.hasParser(param.format)) {
            Dialogs.error(Imabw.form, title, tr("d.openBook.unsupportedFormat", param.format))
        }
        val dialog = Dialogs.waiting(Imabw.form, title, tr("d.openBook.tip", param.file), tr("d.openBook.waiting"))
        Books.openBook(arrayOf(param), {
            task?.cleanup()
        }, object : Observer<Triple<Book, File?, EpmInParam>> {
            override fun onError(e: Throwable) {
                dialog.isVisible = false
            }

            override fun onNext(t: Triple<Book, File?, EpmInParam>) {
            }

            override fun onCompleted() {
                dialog.isVisible = false
            }

        })
        dialog.showForResult(false)
    }

    private fun updateBookState(task: Task) {

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
        val param = EpmInParam.getOrSelect(Imabw.form, title, file, task?.format) ?: return
        param.cached = true
        openBook(title, param)
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
        val book = Books.newBook(Imabw.form, title, name) ?: return
        task?.cleanup()
        task = Task(book) {
            updateBookState(it)
        }
        Imabw.message("d.newBook.done", book)
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
                History.append(task.source, false)
            }
        }
        App.exit(0)
    }
}
