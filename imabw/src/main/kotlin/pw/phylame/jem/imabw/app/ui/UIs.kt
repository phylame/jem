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

package pw.phylame.jem.imabw.app.ui

import pw.phylame.jem.core.Chapter
import pw.phylame.jem.imabw.app.*
import pw.phylame.jem.imabw.app.ui.editor.TabbedEditor
import pw.phylame.jem.imabw.app.ui.tree.ContentsTree
import pw.phylame.qaf.core.Settings
import pw.phylame.qaf.core.tr
import pw.phylame.qaf.ixin.*
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JComponent
import javax.swing.JSplitPane
import javax.swing.WindowConstants

interface Editable {
    val viewer: Viewer

    fun undo()

    fun redo()

    fun cut()

    fun copy()

    fun paste()

    fun delete()

    fun selectAll()

    fun find()

    fun findNext()

    fun findPrevious()

    fun gotoPosition()

    fun updateEditActions(enable: Boolean) {
        val viewer = viewer
        for (command in EDIT_COMMANDS) {
            viewer.actions[command]?.isEnabled = enable
        }
    }

    fun updateFindActions(enable: Boolean) {
        val viewer = viewer
        for (command in FIND_COMMANDS) {
            viewer.actions[command]?.isEnabled = enable
        }
    }
}

class Viewer() : Form(tr("app.name"), Settings("$SETTINGS_HOME/snap")) {
    companion object {
        private const val DIVIDER_SIZE = "form.divider.size"
        private const val DIVIDER_LOCATION = "form.divider.location"
        private const val SIDE_BAR_VISIBLE = "form.sidebar.visible"

        private const val dividerSize = 7
        private const val dividerLocation = 171
    }

    lateinit var splitPane: JSplitPane
        private set

    lateinit var tree: ContentsTree
        private set

    lateinit var editor: TabbedEditor
        private set

    private var activeComponent: JComponent? = null

    override fun init() {
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                Imabw.performed(EXIT_APP)
            }
        })
        iconImage = imageFor(tr("app.icon"))
        createActions()
        createComponents(JSONDesigner(DESIGNER_NAME), Imabw)
        createContent()
        super.init()
        prepare()
        Imabw.addProxy(this)
    }

    private fun createActions() {
        for (cmd in EDIT_COMMANDS) {
            +EditAction(cmd)
        }
        for (cmd in FIND_COMMANDS) {
            +EditAction(cmd)
        }
    }

    private fun createContent() {
        splitPane = JSplitPane()
        splitPane.dividerSize = snap?.get(DIVIDER_SIZE) ?: dividerSize
        splitPane.dividerLocation = snap?.get(DIVIDER_LOCATION) ?: dividerLocation

        tree = ContentsTree(this)
        editor = TabbedEditor(this)

        splitPane.leftComponent = tree
        contentPane.add(splitPane, BorderLayout.CENTER)
    }

    private fun prepare() {
        val toolbar = toolBar
        if (toolbar != null) {
            toolbar.componentPopupMenu = createPopupMenu(arrayOf(
                    Item(LOCK_TOOL_BAR, selected = snap?.get(TOOL_BAR_LOCKED) ?: true, style = Style.CHECK),
                    Item(HIDE_TOOL_BAR_TEXT, selected = snap?.get(TOOL_BAR_TEXT_HIDDEN) ?: true, style = Style.CHECK)),
                    title)

            actions[SHOW_TOOL_BAR]?.isSelected = toolbar.isVisible
            actions[LOCK_TOOL_BAR]?.isSelected = toolbar.isLocked
        }
        val statusbar = statusBar
        if (statusbar != null) {
            actions[SHOW_STATUS_BAR]?.isSelected = statusbar.isVisible
        }

        isSidebarVisible = snap?.get(SIDE_BAR_VISIBLE) ?: true
        actions[SIDE_BAR_VISIBLE]?.isSelected = tree.isVisible
    }

    var isSidebarVisible: Boolean get() = true
        set(value) {
            snap?.set(SIDE_BAR_VISIBLE, value)
            if (value && snap != null) {
                splitPane.dividerSize = snap[DIVIDER_SIZE] ?: dividerSize
                splitPane.dividerLocation = snap[DIVIDER_LOCATION] ?: dividerLocation
            } else {
                snap?.set(DIVIDER_SIZE, splitPane.dividerSize)
                snap?.set(DIVIDER_LOCATION, splitPane.dividerLocation)
                splitPane.dividerSize = 0
                splitPane.dividerLocation = 0
            }
        }

    override fun restoreStatus() {
        super.restoreStatus()
        if (snap != null) {
            splitPane.dividerSize = snap[DIVIDER_SIZE] ?: dividerSize
            splitPane.dividerLocation = snap[DIVIDER_LOCATION] ?: dividerLocation
        }
    }

    override fun saveStatus() {
        super.saveStatus()
        if (snap != null) {

        }
    }

    fun updateBook(chapter: Chapter) {
        tree.updateBook(chapter)
        updateTiele()
    }

    fun updateTiele() {

    }

    @Command(SHOW_TOOL_BAR)
    fun toggleToolbar() {
        val toolbar = toolBar
        if (toolbar != null) {
            toolbar.isVisible = !toolbar.isVisible
        }
    }

    @Command(LOCK_TOOL_BAR)
    fun lockToolbar() {
        val toolbar = toolBar
        if (toolbar != null) {
            toolbar.isLocked = !toolbar.isLocked
        }
    }

    @Command(HIDE_TOOL_BAR_TEXT)
    fun hideToolbarText() {
        val toolbar = toolBar
        if (toolbar != null) {
            toolbar.isTextHidden = !toolbar.isTextHidden
        }
    }

    @Command(SHOW_STATUS_BAR)
    fun toggleStatusbar() {
        val statusbar = statusBar
        if (statusbar != null) {
            statusbar.isVisible = !statusbar.isVisible
        }
    }

    @Command(SHOW_SIDE_BAR)
    fun toggleSidebar() {

    }

    private inner class EditAction(val id: String) : IAction(id) {
        override fun actionPerformed(e: ActionEvent) {
            println(id)
            val comp = activeComponent
            if (comp == null || comp !is Editable) {
                return
            }
        }
    }
}
