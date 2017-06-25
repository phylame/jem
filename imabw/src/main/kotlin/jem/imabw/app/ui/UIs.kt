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

package jem.imabw.app.ui

import jem.Chapter
import jem.imabw.app.*
import jem.imabw.app.ui.editor.TabbedEditor
import jem.imabw.app.ui.tree.ContentsTree
import jem.kotlin.author
import jem.kotlin.title
import qaf.core.App
import qaf.core.Settings
import qaf.ixin.*
import qaf.swing.center
import qaf.swing.east
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.*

interface Editable {
    fun undo()

    fun redo()

    fun cut()

    fun copy()

    fun paste()

    fun delete()

    fun selectAll()

    fun goto()

    fun find()

    fun findNext()

    fun findPrevious()

    fun updateEditActions(enable: Boolean) {
        for (command in EDIT_COMMANDS) {
            Viewer.actions[command]?.isEnabled = enable
        }
    }

    fun updateFindActions(enable: Boolean) {
        for (command in FIND_COMMANDS) {
            Viewer.actions[command]?.isEnabled = enable
        }
    }
}

object Viewer : IForm(App.tr("app.name"), Settings("$SETTINGS_DIR/snap")) {
    private const val DIVIDER_SIZE_KEY = "form.divider.size"
    private const val DIVIDER_LOCATION_KEY = "form.divider.location"
    private const val SIDE_BAR_VISIBLE_KEY = "form.sidebar.visible"

    private const val dividerSize = 7
    private const val dividerLocation = 171

    lateinit var splitPane: JSplitPane
        private set

    lateinit var tree: ContentsTree
        private set

    lateinit var editor: TabbedEditor
        private set

    lateinit var dashboard: Dashboard
        private set

    lateinit var indicator: Indicator
        private set

    private var activeComponent: JComponent? = null

    init {
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                Imabw.performed(EXIT_APP)
            }
        })
        iconImage = imageFor("app.icon")
        createActions()
        createComponents(JSONDesigner(DESIGNER_NAME), Imabw)
        createContent()
        restoreStatus()
        Imabw.addProxy(this)
        showDashboard()
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
        splitPane.dividerSize = snap?.get(DIVIDER_SIZE_KEY) ?: dividerSize
        splitPane.dividerLocation = snap?.get(DIVIDER_LOCATION_KEY) ?: dividerLocation

        tree = ContentsTree
        editor = TabbedEditor
        dashboard = Dashboard

        splitPane.leftComponent = tree
        splitPane.rightComponent = dashboard
        contentPane.center = splitPane
    }

    private fun setRightComponent(com: Component) {
        if (com === splitPane.rightComponent) {
            return
        }
        val size = splitPane.dividerSize
        val location = splitPane.dividerLocation
        splitPane.rightComponent = com
        splitPane.dividerSize = size
        splitPane.dividerLocation = location
    }

    fun showDashboard() {
        setRightComponent(dashboard)
        activeComponent = tree
    }

    var isSidebarVisible: Boolean get() = tree.isVisible
        set(value) {
            snap?.set(SIDE_BAR_VISIBLE_KEY, value)
            if (value && snap != null) {
                splitPane.dividerSize = snap[DIVIDER_SIZE_KEY] ?: dividerSize
                splitPane.dividerLocation = snap[DIVIDER_LOCATION_KEY] ?: dividerLocation
            } else {
                snap?.set(DIVIDER_SIZE_KEY, splitPane.dividerSize)
                snap?.set(DIVIDER_LOCATION_KEY, splitPane.dividerLocation)
                splitPane.dividerSize = 0
                splitPane.dividerLocation = 0
            }
            tree.isVisible = value
        }

    override fun restoreStatus() {
        super.restoreStatus()
        val snap = snap
        if (snap != null) {
            splitPane.dividerSize = snap[DIVIDER_SIZE_KEY] ?: dividerSize
            splitPane.dividerLocation = snap[DIVIDER_LOCATION_KEY] ?: dividerLocation
        }
        val toolbar = toolBar
        if (toolbar != null) {
            toolbar.componentPopupMenu = popupMenu(title,
                    Item(LOCK_TOOL_BAR, selected = snap?.get(TOOL_BAR_LOCKED) ?: true, style = Style.CHECK),
                    Item(HIDE_TOOL_BAR_TEXT, selected = snap?.get(TOOL_BAR_TEXT_HIDDEN) ?: true, style = Style.CHECK)
            )
            actions[SHOW_TOOL_BAR]?.isSelected = toolbar.isVisible
            actions[LOCK_TOOL_BAR]?.isSelected = toolbar.isLocked
        }
        val statusbar = statusBar
        if (statusbar != null) {
            actions[SHOW_STATUS_BAR]?.isSelected = statusbar.isVisible
            indicator = Indicator
            statusbar.east = indicator
        }

        isSidebarVisible = snap?.get(SIDE_BAR_VISIBLE_KEY) ?: true
        actions[SHOW_SIDE_BAR]?.isSelected = tree.isVisible
    }

    override fun saveStatus() {
        super.saveStatus()
        if (snap != null) {

        }
    }

    fun updateBook(chapter: Chapter) {
        editor.clearTabs(false)
        tree.updateBook(chapter)
        updateTitle()
    }

    fun updateHistory() {
        val menu = menus["menu.history"] ?: return
        menu.removeAll()

        var count = 0
        for (path in History.items) {
            menu.add(OpenHistortAction(path).asMenuItem(Style.PLAIN, this))
            ++count
        }

        val action = actions[CLEAR_HISTORY] ?: return
        if (count > 0) {
            menu.addSeparator()
            action.isEnabled = true
        } else {
            action.isEnabled = false
        }

        menu.add(action.asMenuItem(Style.PLAIN, this))
    }

    fun updateTitle() {
        val b = StringBuilder()
        val task = Manager.task
        if (task != null) {
            b.append(task.book.title)
            if (task.isModified) {
                b.append('*')
            }
            b.append(" - ")
            val author = task.book.author
            if (author.isNotEmpty()) {
                b.append('[').append(author).append("] - ")
            }
            val file = task.inParam?.file
            if (file != null) {
                if (task.inParam?.format == null) {
                    b.append('[').append(App.tr("viewer.title.imported")).append("] - ")
                }
                b.append(file.path).append(" - ")
            }
        }
        b.append(App.tr("app.name")).append(" ").append(APP_VERSION)
        title = b.toString()
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
        isSidebarVisible = !isSidebarVisible
    }

    private class OpenHistortAction(val path: String) : AbstractAction() {
        init {
            this[Action.NAME] = path
            this[Action.LONG_DESCRIPTION] = App.tr("openHistory.details", path)
        }

        override fun actionPerformed(e: ActionEvent) {
            Manager.openFile(File(path))
        }
    }

    private class EditAction(val id: String) : IAction(id) {
        override fun actionPerformed(e: ActionEvent) {
            val comp = activeComponent
            if (comp == null || comp !is Editable) {
                return
            }
            comp.javaClass.getMethod(id).invoke(comp)
        }
    }
}
