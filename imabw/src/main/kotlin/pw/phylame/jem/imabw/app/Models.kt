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
import pw.phylame.jem.epm.Helper
import pw.phylame.qaf.core.App
import pw.phylame.qaf.ixin.Command
import java.io.File
import java.util.*

class Task(val updater: (Task) -> Unit,
           val book: Book,
           val source: File? = null,
           val format: String = Helper.PMAB,
           val args: Map<String, Any> = emptyMap()) {

    constructor(updater: (Task) -> kotlin.Unit, book: Book) : this(updater, book, null)

    val isModified: Boolean get() = editedChapters.isNotEmpty()

    private val editedChapters = LinkedList<Chapter>()

    private fun notifyState(state: State) {
        if (state.isModified) {
            editedChapters.add(state.chapter)
        } else {
            editedChapters.remove(state.chapter)
        }
        updater(this)
    }

    private val states = IdentityHashMap<Chapter, State>()

    private fun getOrNew(chapter: Chapter): State = states.getOrPut(chapter) { State(chapter) }

    fun textModified(chapter: Chapter, modified: Boolean) {
        val state = getOrNew(chapter)
        state.setTextModified(modified)
        notifyState(state)
    }

    fun childrenModified(chapter: Chapter, modified: Boolean) {
        val state = getOrNew(chapter)
        state.setChildrenModified(modified)
        notifyState(state)
    }

    fun attributeModified(chapter: Chapter, modified: Boolean) {
        val state = getOrNew(chapter)
        state.setAttributeModified(modified)
        notifyState(state)
    }

    fun extensionModified(book: Book, modified: Boolean) {
        val state = getOrNew(book)
        state.setExtensionModified(modified)
        notifyState(state)
    }

    fun isModified(chapter: Chapter): Boolean = states[chapter]?.isModified ?: false

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

object Manager {
    private val _tasks: MutableList<Task> = LinkedList()

    // readonly view of tasks
    val tasks: List<Task> get() = _tasks

    @Command
    fun openFile() {

    }

    @Command
    fun newFile() {

    }

    @Command
    fun saveFile() {

    }

    @Command
    fun saveAsFile() {

    }

    @Command
    fun exitApp() {
        App.exit(0)
    }
}
