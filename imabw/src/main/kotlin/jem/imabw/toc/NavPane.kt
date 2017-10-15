/*
 * Copyright 2015-2017 Peng Wan <phylame@163.com>
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

package jem.imabw.toc

import javafx.beans.binding.Bindings
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.util.Callback
import jclp.isRoot
import jclp.log.Log
import jem.Book
import jem.Chapter
import jem.epm.ParserParam
import jem.imabw.Imabw
import jem.imabw.LoadTextTask
import jem.imabw.Workbench
import jem.imabw.editor.EditorPane
import jem.imabw.loadBook
import jem.imabw.ui.*
import jem.intro
import jem.title
import mala.App
import mala.App.tr
import mala.ixin.*
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger

typealias ChapterNode = TreeItem<Chapter>

object NavPane : BorderPane(), CommandHandler {
    private const val TAG = "Nav"

    val treeView: TreeView<Chapter> = object : TreeView<Chapter>(ChapterNode()), Editable {
        override fun onEdit(command: String) {
            handle(command, this)
        }
    }

    val selection: ObservableList<TreeItem<Chapter>> get() = treeView.selectionModel.selectedItems

    val selectedChapter: Chapter? get() = treeView.selectionModel.selectedItem?.value

    init {
        Imabw.register(this)

        id = "nav-pane"
        top = NavHeader
        center = treeView

        initTree()
        initActions()

        Workbench.workProperty.addListener { _, _, work ->
            treeView.root = createNode(work.book)
            treeView.selectionModel.select(0)
            treeView.root.isExpanded = true
            treeView.requestFocus()
        }
    }

    private fun initTree() {
        val tree = this.treeView
        tree.id = "toc-tree"
        tree.cellFactory = CellFactory
        tree.selectionModel.selectionMode = SelectionMode.MULTIPLE
        Imabw.dashboard.appDesigner.items["navContext"]?.let {
            tree.contextMenu = it.toContextMenu(Imabw, App, App.assets, IxIn.actionMap, null)
        }
        tree.addEventHandler(MouseEvent.MOUSE_PRESSED) { event ->
            if (event.clickCount == 2 && event.isPrimaryButtonDown) {
                treeView.selectionModel.selectedItem?.takeIf { it.isLeaf && it.isNotRoot }?.let {
                    EditorPane.openText(it.value, CellFactory.getIcon(it))
                    event.consume()
                }
            }
        }
        tree.addEventHandler(KeyEvent.KEY_PRESSED) { event ->
            // only ENTER pressed
            if (!event.isShortcutDown && !event.isShiftDown && !event.isAltDown && event.code == KeyCode.ENTER) {
                selection.forEach {
                    if (!it.isLeaf) {
                        it.isExpanded = !it.isExpanded
                        event.consume()
                    } else if (it.isNotRoot) {
                        EditorPane.openText(it.value, CellFactory.getIcon(it))
                        event.consume()
                    }
                }
            }
        }
    }

    private fun initActions() {
        val hasBook = Bindings.createBooleanBinding(Callable { selection.any { it?.isRoot == true } }, selection)

        val empty = Bindings.isEmpty(selection)
        val multiple = Bindings.size(selection).greaterThan(1)
        val emptyOrBook = empty.or(hasBook)
        val emptyOrMultiple = empty.or(multiple)

        val actionMap = IxIn.actionMap
        actionMap["newChapter"]?.disableProperty?.bind(empty)
        actionMap["importChapter"]?.disableProperty?.bind(empty)
        actionMap["insertChapter"]?.disableProperty?.bind(emptyOrBook)
        actionMap["exportChapter"]?.disableProperty?.bind(emptyOrBook)
        actionMap["renameChapter"]?.disableProperty?.bind(emptyOrMultiple)
        actionMap["editText"]?.disableProperty?.bind(empty)
        actionMap["moveChapter"]?.disableProperty?.bind(emptyOrBook)
        actionMap["mergeChapter"]?.disableProperty?.bind(emptyOrBook.or(multiple.not()))
        actionMap["viewAttributes"]?.disableProperty?.bind(emptyOrMultiple)

        actionMap["gotoChapter"]?.disableProperty?.bind(Bindings.createBooleanBinding(Callable {
            EditorPane.selectedTab == null
        }, EditorPane.selectionModel.selectedItemProperty()))

        sceneProperty().addListener { _, _, scene ->
            scene?.focusOwnerProperty()?.addListener { _, _, new ->
                val actions = arrayOf("undo", "redo", "cut", "copy", "paste", "find", "findNext", "findPrevious")
                if (new === treeView) {
                    for (action in actions) {
                        actionMap[action]?.let {
                            it.disableProperty.unbind()
                            it.isDisable = true
                        }
                    }
                    actionMap["delete"]?.disableProperty?.bind(hasBook)
                }
            }
        }
    }

    fun createChapter(): Chapter? {
        return inputText(tr("d.newChapter.title"), tr("d.newChapter.tip"), tr("chapter.untitled"))?.let {
            Chapter(it)
        }
    }

    fun locateChapter(chapter: Chapter) {
        val node = locateNode(chapter, treeView.root)
        if (node == null) {
            Log.d(TAG) { "Not found $chapter in nav" }
        } else {
            treeView.selectAndScrollTo(node)
        }
    }

    @Command
    fun renameChapter() {
        val treeItem = treeView.selectionModel.selectedItem
        val chapter = treeItem!!.value
        inputText(tr("d.renameChapter.title"), tr("d.renameChapter.tip"), chapter.title)?.let {
            chapter.title = it
            treeItem.refresh()
            if (chapter.isRoot) {
                Workbench.work.titleProperty.value = it
            }
            Workbench.work.isModified = true
        }
    }

    @Command
    fun importChapter() {
        val files = openBookFiles() ?: return
        val targets = selection.toList()
        val counter = AtomicInteger()
        val succeed = AtomicInteger()
        val fxApp = Imabw.fxApp
        fxApp.showProgress()
        for (file in files) {
            val param = ParserParam(file.path)
            val task = object : Task<Book>() {
                override fun call() = loadBook(param)
            }
            task.setOnRunning {
                fxApp.updateProgress(tr("jem.loadBook.hint", param.path))
            }
            task.setOnSucceeded {
                succeed.incrementAndGet()
                insertNodes(listOf(createNode(task.value)), targets, InsertMode.TO_PARENT)
                if (counter.incrementAndGet() == files.size) {
                    fxApp.hideProgress()
                    Imabw.message(tr("d.importChapter.result", succeed))
                }
            }
            task.setOnFailed {
                Log.d("importChapter", task.exception) { "failed to load book: ${param.path}" }
                if (counter.incrementAndGet() == files.size) {
                    fxApp.hideProgress()
                    Imabw.message(tr("d.importChapter.result", succeed))
                }
            }
            Imabw.submit(task)
        }
    }

    fun createNode(chapter: Chapter) = chapter.toTreeItem()

    fun insertNodes(sources: Collection<ChapterNode>, targets: Collection<ChapterNode>, mode: InsertMode) {
        if (sources.isEmpty() || targets.isEmpty()) return
        require(sources.none { it in targets }) { "Cannot insert node to self" }
        val dump = targets.takeIf { it !== selection } ?: targets.toList()
        val model = treeView.selectionModel.apply { clearSelection() }
        dump.forEachIndexed { index, target ->
            // clone chapter(s) except the first one
            val items = if (index == 0) sources else sources.map { createNode(it.value.clone()) }
            when (mode) {
                InsertMode.BEFORE_ITEM -> target.parent.let { insertNodes(items, it, it.children.indexOf(target)) }
                InsertMode.AFTER_ITEM -> target.parent.let { insertNodes(items, it, it.children.indexOf(target) + 1) }
                InsertMode.TO_PARENT -> insertNodes(items, target, -1)
            }
            items.forEach { model.select(it) }
        }
        treeView.scrollTo(model.selectedIndices.last())
        Workbench.work.isModified = true
    }

    fun insertNodes(sources: Collection<ChapterNode>, target: ChapterNode, index: Int) {
        if (sources.isEmpty()) return
        val parent = target.value
        if (index < 0) {
            target.children += sources.onEach { parent += it.value }
        } else {
            target.children.addAll(index, sources.onEach { parent += it.value })
        }
    }

    fun removeNodes(nodes: Collection<ChapterNode>) {
        nodes.reversed().forEach { node ->
            val chapter = node.value
            val parentNode = node.parent
            parentNode.children.remove(node.apply { parentNode.value.remove(chapter) })
            EditorPane.closeText(chapter)
            Imabw.submit { chapter.cleanup() }
        }
        Workbench.work.isModified = true
    }

    fun locateNode(chapter: Chapter, node: ChapterNode): ChapterNode? {
        if (node.value === chapter) {
            return node
        }
        for (item in node.children) {
            return locateNode(chapter, item) ?: continue
        }
        return null
    }

    fun collapseNode(node: ChapterNode) {
        if (node.isExpanded) {
            node.children.forEach { collapseNode(it) }
        }
        node.isExpanded = false
    }

    override fun handle(command: String, source: Any): Boolean {
        when (command) {
            "delete" -> removeNodes(selection)
            "selectAll" -> treeView.selectionModel.selectAll()
            "editText" -> selection.forEach { EditorPane.openText(it.value, CellFactory.getIcon(it)) }
            "newChapter" -> createChapter()?.let {
                insertNodes(listOf(createNode(it)), selection, InsertMode.TO_PARENT)
            }
            "insertChapter" -> createChapter()?.let {
                insertNodes(listOf(createNode(it)), selection, InsertMode.BEFORE_ITEM)
            }
            "exportChapter" -> Workbench.exportFiles(selection.map { it.value })
            "viewAttributes" -> editAttributes(selectedChapter!!)
            "bookAttributes" -> editAttributes(Workbench.work.book)
            "bookExtensions" -> Workbench.work.book.let {
                editVariants(it.extensions, tr("d.editExtension.title", it.title))
            }
            "gotoChapter" -> EditorPane.selectedTab?.chapter?.let {
                locateChapter(it)
                treeView.requestFocus()
            }
            "collapseToc" -> collapseNode(treeView.root)
            else -> return false
        }
        return true
    }
}

object NavHeader : BorderPane() {
    init {
        id = "nav-header"
        left = Label(tr("form.nav.title"), App.assets.graphicFor("tree/contents")).also {
            it.id = "nav-label"
            BorderPane.setAlignment(it, Pos.CENTER)
        }

        right = ToolBar().also {
            it.id = "nav-tool-bar"
            Imabw.dashboard.appDesigner.items["navTools"]?.let { items ->
                it.init(items, Imabw, App, App.assets, IxIn.actionMap)
            }
            BorderPane.setAlignment(it, Pos.CENTER)
        }
    }
}

object CellFactory : Callback<TreeView<Chapter>, TreeCell<Chapter>> {
    val bookIcon = App.assets.imageFor("tree/book")!!
    val sectionIcon = App.assets.imageFor("tree/section")!!
    val chapterIcon = App.assets.imageFor("tree/chapter")!!

    fun getIcon(node: ChapterNode) = ImageView(when {
        node.isRoot -> bookIcon
        !node.isLeaf -> sectionIcon
        else -> chapterIcon
    })

    fun getIcon(chapter: Chapter) = ImageView(when {
        chapter.isRoot -> bookIcon
        chapter.isSection -> sectionIcon
        else -> chapterIcon
    })

    override fun call(param: TreeView<Chapter>): TreeCell<Chapter> {
        return ChapterCell()
    }
}

private class ChapterCell : TreeCell<Chapter>() {
    override fun updateItem(chapter: Chapter?, empty: Boolean) {
        super.updateItem(chapter, empty)
        text = chapter?.title
        graphic = when {
            empty || chapter == null -> null
            chapter.isRoot -> ImageView(CellFactory.bookIcon)
            chapter.isSection -> ImageView(CellFactory.sectionIcon)
            else -> ImageView(CellFactory.chapterIcon)
        }
        tooltip = null
        chapter?.intro?.let {
            val intro = chapter.tag as? String
            if (intro != null) {
                tooltip = createTooltip(intro)
            } else if (chapter.tag == null) {
                chapter.tag = this
                with(LoadTextTask(it)) {
                    setOnSucceeded {
                        value.takeIf { it.isNotEmpty() }?.let { text ->
                            tooltip = createTooltip(text)
                            chapter.tag = text
                        }
                        hideProgress()
                    }
                    Imabw.submit(this)
                }
            }
        }
    }

    private fun createTooltip(text: String) = Tooltip(text).also {
        it.isWrapText = true
        it.styleClass += "intro-tooltip"
        it.maxWidthProperty().bind(widthProperty().multiply(1.2))
    }
}

enum class InsertMode {
    BEFORE_ITEM,
    AFTER_ITEM,
    TO_PARENT
}
