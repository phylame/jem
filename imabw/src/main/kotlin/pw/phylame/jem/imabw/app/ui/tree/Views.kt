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

package pw.phylame.jem.imabw.app.ui.tree

import org.jdesktop.swingx.JXLabel
import org.jdesktop.swingx.JXPanel
import org.jdesktop.swingx.JXTree
import org.json.JSONArray
import org.json.JSONTokener
import pw.phylame.jem.imabw.app.*
import pw.phylame.jem.imabw.app.ui.Performer
import pw.phylame.qaf.core.tr
import pw.phylame.qaf.ixin.*
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*

object ContentsTree : JXPanel(BorderLayout()) {
    private var model = BookModel()

    private lateinit var tree: JXTree

    private lateinit var menu: JPopupMenu

    fun init(performer: Performer) {
        addActions(performer)
        initUI()
    }

    private fun addActions(performer: Performer) {
        val actions = performer.actions
        actions[NEW_CHAPTER] = Item(NEW_CHAPTER).asAction(Imabw)
        actions[INSERT_CHAPTER] = Item(INSERT_CHAPTER).asAction(Imabw)
        actions[IMPORT_CHAPTER] = Item(IMPORT_CHAPTER).asAction(Imabw)
        actions[EXPORT_CHAPTER] = Item(EXPORT_CHAPTER).asAction(Imabw)
        actions[RENAME_CHAPTER] = Item(RENAME_CHAPTER).asAction(Imabw)
        actions[MERGE_CHAPTER] = Item(MERGE_CHAPTER).asAction(Imabw)
        actions[LOCK_CONTENTS] = Item(LOCK_CONTENTS).asAction(Imabw)
        actions[CHAPTER_PROPERTIES] = Item(CHAPTER_PROPERTIES).asAction(Imabw)
        actions[BOOK_EXTENSIONS] = Item(BOOK_EXTENSIONS).asAction(Imabw)
    }

    private fun initUI() {
        add(ControlsPane, BorderLayout.PAGE_START)
        tree = JXTree(model)
        add(JScrollPane(tree), BorderLayout.CENTER)
        tree.isEditable = false
        tree.dragEnabled = true
        tree.dropMode = DropMode.USE_SELECTION
        fileFor("ui/contents.json")?.openStream()?.use {
            val items = LinkedList<Item>()
            JSONDesigner.parseItems(JSONArray(JSONTokener(it)), items)
            menu = Performer.createPopupMenu(items.toTypedArray())
            tree.componentPopupMenu = menu
        }
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
//                if (e.clickCount !== 1) {
//                    return
//                }
//                if (SwingUtilities.isLeftMouseButton(e)) {
//                    if (!tree.isFocusOwner) {
//                        tree.requestFocus()
//                    }
//                }
//                if (!e.isControlDown) {
//                    val path = tree.getPathForLocation(e.x, e.y)
//                    // select the path if not in selection
//                    if (path != null && !tree.isPathSelected(path)) {
//                        tree.selectionPath = path
//                    }
//                }
//                if (SwingUtilities.isRightMouseButton(e)) {
//                    menu.show(tree, e.x, e.y)
//                }
            }

            override fun mousePressed(e: MouseEvent) {
                if (e.clickCount === 2 && SwingUtilities.isLeftMouseButton(e)) {
                    val path = tree.getPathForLocation(e.x, e.y)
                    if (path != null) {
//                        editContent(path)
                    }
                }

            }
        })
    }

    object Clipboard {
    }
}

object ControlsPane : JXPanel(BorderLayout()) {
    val toolbar = JToolBar()

    init {
        initToolbar()
        add(JXLabel(tr("contents.title"), iconFor("tree/contents.png"), JXLabel.LEADING), BorderLayout.LINE_START)
        add(toolbar, BorderLayout.LINE_END)
    }

    private fun initToolbar() {
        toolbar.isRollover = true
        toolbar.isLocked = true
        toolbar.isBorderPainted = false
        toolbar.addSeparator()
        addButton(NEW_CHAPTER, Style.PLAIN)
        addButton(CHAPTER_PROPERTIES, Style.PLAIN)
        addButton(LOCK_CONTENTS, Style.TOGGLE)
    }

    private fun addButton(id: String, style: Style) {
        toolbar.addButton(Item(id, style = style).asAction(Imabw).asButton(style))
    }
}

