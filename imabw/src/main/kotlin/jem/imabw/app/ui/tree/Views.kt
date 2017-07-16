/*
 * Copyright 2017 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
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

package jem.imabw.app.ui.tree

import jem.Chapter
import jem.imabw.app.Imabw
import jem.imabw.app.Manager
import jem.imabw.app.ui.Editable
import jem.imabw.app.ui.Viewer
import jem.kotlin.title
import org.jdesktop.swingx.JXLabel
import org.jdesktop.swingx.JXPanel
import org.jdesktop.swingx.JXTree
import org.json.JSONObject
import org.json.JSONTokener
import qaf.core.App
import qaf.ixin.*
import qaf.swing.center
import qaf.swing.east
import qaf.swing.north
import qaf.swing.west
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreePath

object BookRender : DefaultTreeCellRenderer() {
    private val bookIcon by lazy {
        iconFor("tree/book.png")
    }

    private val sectionIcon by lazy {
        iconFor("tree/section.png")
    }

    private val chapterIcon by lazy {
        iconFor("tree/book.png")
    }

    private val highlightColor = Color.BLUE

    private val copyingColor = Color.LIGHT_GRAY

    private val cuttingColor = Color.DARK_GRAY

    private val defaultFont by lazy {
        font
    }

    init {
        leafIcon = chapterIcon
        openIcon = sectionIcon
        closedIcon = sectionIcon
    }

    override fun getTreeCellRendererComponent(tree: JTree,
                                              value: Any,
                                              selected: Boolean,
                                              expanded: Boolean,
                                              leaf: Boolean,
                                              row: Int,
                                              hasFocus: Boolean): Component {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
        val chapter = value as Chapter
        if (chapter.isRoot) {
            icon = bookIcon
        } else if (Manager.task?.isModifiedOf(chapter) ?: false) {
            foreground = highlightColor
            font = defaultFont.deriveFont(Font.BOLD)
        } else if (chapter in Clipboard) { // in clipboard
            foreground = if (Clipboard.isCopying) copyingColor else cuttingColor
        } else {
            font = defaultFont
        }
        text = chapter.title
        return this
    }
}

object ControlsPane : JXPanel(BorderLayout()) {
    private val toolbar = JToolBar()

    init {
        toolbar.isRollover = true
        toolbar.isLocked = true
        toolbar.isBorderPainted = false
        west = JXLabel(App.tr("contents.title"), iconFor("tree/contents.png"), JXLabel.LEADING)
        east = toolbar
    }

    internal fun initTools(items: Array<Item>) {
        toolbar.addItems(items, Viewer.actions, Imabw)
        toolbar.components
                .filterIsInstance<AbstractButton>()
                .forEach {
                    it.icon = it.action[Action.SMALL_ICON]
                }
    }
}

object ContentsTree : JXPanel(BorderLayout()), Editable {
    lateinit var tree: JXTree
        private set

    lateinit var menu: JPopupMenu
        private set

    init {
        initUI()
        Imabw.addProxy(this)
    }

    private fun initUI() {
        tree = JXTree(BookModel)
        center = JScrollPane(tree)

        tree.isEditable = false
        tree.dragEnabled = true
        tree.isRolloverEnabled = true
        tree.dropMode = DropMode.USE_SELECTION

        tree.cellRenderer = BookRender

        fileFor("ui/contents.json")?.openStream()?.use {
            var items = LinkedList<Item>()
            val json = JSONObject(JSONTokener(it))
            var array = json.optJSONArray("menu")
            if (array != null) {
                JSONDesigner.parseItems(array, items)
                menu = Viewer.popupMenu("", *items.toTypedArray())
            }
            items = LinkedList<Item>()
            array = json.optJSONArray("toolbar")
            if (array != null) {
                JSONDesigner.parseItems(array, items)
                ControlsPane.initTools(items.toTypedArray())
                north = ControlsPane
            }
        }

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                if (e.clickCount !== 1) {
                    return
                }
                if (e.isLeft) {
                    if (!tree.isFocusOwner) {
                        tree.requestFocus()
                    }
                }
                if (!e.isControlDown) {
                    val path = tree.getPathForLocation(e.x, e.y)
                    // select the path if not in selection
                    if (path != null && !tree.isPathSelected(path)) {
                        tree.selectionPath = path
                    }
                }
                if (e.isRight) {
                    menu.show(tree, e.x, e.y)
                }
            }

            override fun mousePressed(e: MouseEvent) {
                if (e.clickCount === 2 && e.isLeft) {
                    val path = tree.getPathForLocation(e.x, e.y)
                    if (path != null) {
                        editContent(path)
                    }
                }
            }
        })
    }

    fun updateBook(chapter: Chapter) {
        BookModel.book = chapter
        tree.setSelectionRow(0)
    }

    fun editContent(chapter: Chapter) {
        if (!chapter.isSection) {
//            val tabbedEditor = viewer.getTabbedEditor()
//            var tab = tabbedEditor.findTab(book)
//            if (tab == null) {
//                tab = tabbedEditor.newTab(book, generateChapterTip(book))
//            }
//            tabbedEditor.activateTab(tab)
        }
    }

    fun editContent(path: TreePath) {
        editContent(path.chapter!!)
    }

    override fun undo() {

    }

    override fun redo() {

    }

    override fun cut() {

    }

    override fun copy() {

    }

    override fun paste() {

    }

    override fun delete() {

    }

    override fun selectAll() {

    }

    override fun find() {

    }

    override fun findNext() {

    }

    override fun findPrevious() {

    }

    override fun goto() {

    }

}
