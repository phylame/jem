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
import org.json.JSONObject
import org.json.JSONTokener
import pw.phylame.jem.core.Chapter
import pw.phylame.jem.imabw.app.Imabw
import pw.phylame.jem.imabw.app.ui.Editable
import pw.phylame.jem.imabw.app.ui.Viewer
import pw.phylame.qaf.core.tr
import pw.phylame.qaf.ixin.*
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.tree.TreePath

class ContentsTree(override val viewer: Viewer) : JXPanel(BorderLayout()), Editable {

    private var model = BookModel()

    lateinit var tree: JXTree
        private set

    lateinit var menu: JPopupMenu
        private set

    init {
        initUI()
        Imabw.addProxy(this)
    }

    private fun initUI() {
        tree = JXTree(model)
        add(JScrollPane(tree), BorderLayout.CENTER)

        tree.isEditable = false
        tree.isRolloverEnabled = true
        tree.dragEnabled = true
        tree.dropMode = DropMode.USE_SELECTION

        fileFor("ui/contents.json")?.openStream()?.use {
            var items = LinkedList<Item>()
            val jo = JSONObject(JSONTokener(it))
            var array = jo.optJSONArray("menu");
            if (array != null) {
                JSONDesigner.parseItems(array, items)
                menu = viewer.createPopupMenu(items.toTypedArray())
            }
            items = LinkedList<Item>()
            array = jo.optJSONArray("toolbar")
            if (array != null) {
                JSONDesigner.parseItems(array, items)
                add(ControlsPane(this, items.toTypedArray()), BorderLayout.PAGE_START)
            }
        }

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                if (e.clickCount !== 1) {
                    return
                }
                if (SwingUtilities.isLeftMouseButton(e)) {
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
                if (SwingUtilities.isRightMouseButton(e)) {
                    menu.show(tree, e.x, e.y)
                }
            }

            override fun mousePressed(e: MouseEvent) {
                if (e.clickCount === 2 && SwingUtilities.isLeftMouseButton(e)) {
                    val path = tree.getPathForLocation(e.x, e.y)
                    if (path != null) {
                        editContent(path)
                    }
                }

            }
        })
    }

    fun updateBook(chapter: Chapter) {
        model.update(chapter)
    }

    fun editContent(chapter: Chapter) {
        if (!chapter.isSection) {
//            val tabbedEditor = viewer.getTabbedEditor()
//            var tab = tabbedEditor.findTab(chapter)
//            if (tab == null) {
//                tab = tabbedEditor.newTab(chapter, generateChapterTip(chapter))
//            }
//            tabbedEditor.activateTab(tab)
        }
    }

    fun editContent(path: TreePath) {
        editContent(path.myChapter!!)
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

class Clipboard {
    operator fun contains(chapter: Chapter): Boolean = true

    val isCopying = true
}
//
//class BookCellRender : DefaultTreeCellRenderer() {
//    companion object {
//        val bookIcon = iconFor("tree/book.png")
//
//        val sectionIcon = iconFor("tree/section.png")
//        val chapterIcon = iconFor("tree/chapter.png")
//    }
//
//    private val defaultColor = foreground
//
//    internal var clipboard: Clipboard? = null
//
//    fun getTreeCellRendererComponent(tree: JTree,
//                                     value: Any,
//                                     selected: Boolean,
//                                     expanded: Boolean,
//                                     leaf: Boolean,
//                                     row: Int,
//                                     hasFocus: Boolean): Component {
//        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
//        val chapter = value as Chapter
//        if (chapter.isRoot) {   // book
//            icon = bookIcon
//        } else if (leaf) {
//            icon = chapterIcon
//        } else {
//            icon = sectionIcon
//        }
//        val color: Color
//        if (app.getManager().getActiveTask().isChapterModified(chapter)) {
//            color = highlightColor
//        } else if (clipboard!!.contains(chapter)) {
//            color = if (clipboard!!.isCopying) copyColor else cutColor
//        } else {
//            color = defaultColor
//        }
//        foreground = color
//        return this
//    }
//
//    companion object {
//        private val highlightColor = Color.BLUE
//        private val copyColor = Color.LIGHT_GRAY
//        private val cutColor = Color.DARK_GRAY
//    }
//}

class ControlsPane(val tree: ContentsTree, items: Array<Item>) : JXPanel(BorderLayout()) {
    val toolbar = JToolBar()

    init {
        toolbar.isRollover = true
        toolbar.isLocked = true
        toolbar.isBorderPainted = false
        toolbar.addItems(items, tree.viewer.actions, Imabw)
        for (comp in toolbar.components) {
            if (comp is AbstractButton) {
                comp.icon = comp.action[Action.SMALL_ICON]
            }
        }
        add(JXLabel(tr("contents.title"), iconFor("tree/contents.png"), JXLabel.LEADING), BorderLayout.LINE_START)
        add(toolbar, BorderLayout.LINE_END)
    }
}

