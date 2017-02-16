/*
 * Copyright 2014-2017 Peng Wan <phylame@163.com>
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

package jem.imabw.app.ui.editor

import jem.Chapter
import jem.imabw.app.*
import jem.imabw.app.ui.Viewer
import jem.title
import jem.util.flob.Flobs
import jem.util.text.Texts
import org.jdesktop.swingx.JXPanel
import org.jdesktop.swingx.JXTextArea
import pw.phylame.commons.function.Consumer
import pw.phylame.commons.log.Log
import pw.phylame.qaf.core.App
import pw.phylame.qaf.ixin.isRight
import pw.phylame.qaf.swing.center
import java.awt.BorderLayout
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.lang.ref.WeakReference
import java.nio.charset.Charset
import java.util.*
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.BadLocationException
import javax.swing.undo.UndoManager


class Tab(val chapter: Chapter) : Consumer<Chapter> {
    companion object {
        const val TAG = "Tab"
        const val CACHE_ENCODING = "UTF-16BE"
        val caches = IdentityHashMap<Chapter, File>()
    }

    var isModified: Boolean = false
        private set

    private var cache: File? = null

    fun fireTextModified() {
        isModified = true
        Manager.task?.fireTextModified(chapter, true)
    }

    fun cacheIfNeed() {
        if (isModified) {
            cacheText()
        }
    }

    fun cacheText() {
        cache = makeCache()
        cache?.bufferedWriter(Charset.forName(CACHE_ENCODING))
                ?.use {
                    // todo: write text to file
                    isModified = false
                }
    }

    private fun makeCache(): File = caches.getOrPut(chapter) {
        val file = File.createTempFile("_imabw_chapter_", ".tmp")
        chapter.text = Texts.forFlob(Flobs.forFile(file, "text/plain"), CACHE_ENCODING, Texts.PLAIN)
        chapter.registerCleanup(this)
        file
    }

    override fun consume(chapter: Chapter) {
        val cache = cache
        if (cache != null) {
            if (!cache.delete()) {
                Log.e(TAG, "cannot delete cache file: ${cache.path}")
            }
            if (caches.remove(chapter) == null) {
                Log.e(TAG, "cannot remove book cache: ${chapter.title}")
            }
        }
    }
}

class Editor(tab: Tab, text: String) : JXPanel(BorderLayout()) {
    private val tab = WeakReference(tab)
    private val text: JXTextArea = JXTextArea(text)
    private val undoManager = UndoManager()
    private lateinit var contextMenu: JPopupMenu

    // prohibit notify book modified when text changed
    private var prohibitNotify = false

    val currentRow: Int get() {
        var row = -1
        try {
            row = text.getLineOfOffset(text.caretPosition)
        } catch (e: BadLocationException) {
            App.error("cannot get current row", e)
        }
        return row
    }

    val currentColumn: Int get() {
        var column = -1
        try {
            val row = text.getLineOfOffset(text.caretPosition)
            column = text.caretPosition - text.getLineStartOffset(row)
        } catch (e: BadLocationException) {
            App.error("cannot get current column", e)
        }
        return column
    }

    val selectionCount: Int get() = text.selectionEnd - text.selectionStart

    var isReadonly: Boolean get() = !text.isEditable
        set(value) {
            text.isEditable = !value
            updateCorrelatedActions()
        }

    init {
        initUI()
    }

    private fun initUI() {
        center = JScrollPane(text)

        text.font = EditorSettings.font
        text.background = EditorSettings.background
        text.foreground = EditorSettings.foreground
        text.lineWrap = EditorSettings.isLineWrap
        text.wrapStyleWord = EditorSettings.isWordWrap

        val doc = text.document
        doc.addUndoableEditListener {
            undoManager.addEdit(it.edit)
            // todo: update undo redo action
        }

        doc.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent) {}

            override fun insertUpdate(e: DocumentEvent) {
                if (!prohibitNotify) {
                    tab.get().fireTextModified()
                }
            }

            override fun removeUpdate(e: DocumentEvent) {
                if (!prohibitNotify) {
                    tab.get().fireTextModified()
                }
            }
        })

        text.addCaretListener {
            // todo: update actions, update indicator
        }

        text.componentPopupMenu = null
        text.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                if (e.clickCount !== 1) {
                    return
                }
                if (!text.isFocusOwner) {
                    text.requestFocus()
                }
                val position = text.viewToModel(e.point)
                // if not in selection then update new caret
                if (position < text.selectionStart || position > text.selectionEnd) {
                    text.caretPosition = position
                }
                if (e.isRight) {
                    contextMenu.show(text, e.x, e.y)
                }
            }
        })

        text.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                // todo: activate tabbed editor
                // todo: update actions, update indicator
            }
        })
    }

    private fun updateCorrelatedActions() {
        val viewer = Viewer
        // disable all common edit actions
        viewer.editor.updateEditActions(false)

        // disable all text editor actions
//        viewer.editor.updateTextActions(false)

        viewer.actions[SELECT_ALL]?.isEnabled = true

        if (!text.isEditable) {
            // has text selection
            if (selectionCount > 0) {
                // can copy
                viewer.actions[COPY]?.isEnabled = true
            }
        } else {
//            updateUndoRedoActions()

            // has text selection
            if (selectionCount > 0) {
                viewer.actions[CUT]?.isEnabled = true
                viewer.actions[COPY]?.isEnabled = true

                viewer.actions[TO_LOWER]?.isEnabled = true
                viewer.actions[TO_UPPER]?.isEnabled = true
                viewer.actions[TO_CAPITALIZED]?.isEnabled = true
                viewer.actions[TO_TITLED]?.isEnabled = true
            }

//            viewer.actions[PASTE, canPaste())
            viewer.actions[DELETE]?.isEnabled = true

            viewer.actions[JOIN_LINES]?.isEnabled = true

            viewer.actions[REPLACE]?.isEnabled = true
        }
    }

}
