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

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import javafx.util.Callback
import jclp.EventBus
import jclp.isRoot
import jclp.log.Log
import jem.Book
import jem.Chapter
import jem.epm.ParserParam
import jem.imabw.*
import jem.imabw.editor.EditorPane
import jem.imabw.ui.*
import jem.intro
import jem.title
import mala.App
import mala.App.tr
import mala.ixin.*
import org.fxmisc.wellbehaved.event.EventPattern.mouseClicked
import org.fxmisc.wellbehaved.event.InputMap.consume
import org.fxmisc.wellbehaved.event.Nodes
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger

typealias ChapterNode = TreeItem<Chapter>

object NavPane : BorderPane(), CommandHandler {
    private const val TAG = "Nav"

    private val tree: TreeView<Chapter> = object : TreeView<Chapter>(ChapterNode()), Editable {
        override fun onEdit(command: String) {
            handle(command, this)
        }
    }

    init {
        Imabw.register(this)

        id = "nav-pane"
        top = NavHeader
        center = tree

        initTree()
        initActions()

        EventBus.register<WorkflowEvent> {
            if (it.what == WorkflowType.BOOK_CREATED || it.what == WorkflowType.BOOK_OPENED) {
                tree.root = it.book.toTreeItem()
                tree.selectionModel.select(0)
                tree.root.isExpanded = true
                tree.requestFocus()
            }
        }
    }

    private fun initTree() {
        val tree = this.tree
        tree.id = "toc-tree"
        tree.cellFactory = CellFactory
        tree.selectionModel.selectionMode = SelectionMode.MULTIPLE
        Imabw.dashboard.designer.items["navContext"]?.let {
            tree.contextMenu = it.toContextMenu(Imabw, App, App.assets, IxIn.actionMap, null)
        }
        Nodes.addInputMap(tree, consume(mouseClicked().onlyIf { it.clickCount == 2 && it.button == MouseButton.PRIMARY }, {
            selectedNode?.takeIf { it.isLeaf && it.isNotRoot }?.let {
                EditorPane.openTab(it.value, CellFactory.getIcon(it))
            }
        }))
    }

    private fun initActions() {
        val selection = selectedNodes
        val hasBook = Bindings.createBooleanBinding(Callable { selection.any { it.isRoot } }, selection)
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
        actionMap["editText"]?.disableProperty?.bind(emptyOrBook)
        actionMap["moveChapter"]?.disableProperty?.bind(emptyOrBook)
        actionMap["mergeChapter"]?.disableProperty?.bind(emptyOrBook.or(multiple.not()))
        actionMap["viewAttributes"]?.disableProperty?.bind(emptyOrMultiple)
        actionMap["gotoChapter"]?.disableProperty?.bind(Bindings.createBooleanBinding(Callable {
            EditorPane.selectedTab == null
        }, EditorPane.selectionModel.selectedItemProperty()))

        tree.focusedProperty().addListener { _, _, focused ->
            if (focused) {
                val actions = arrayOf("undo", "redo", "cut", "copy", "paste", "replace")
                for (action in actions) {
                    actionMap[action]?.resetDisable(true)
                }
                actionMap["delete"]?.disableProperty?.bind(hasBook)
                actionMap["find"]?.resetDisable()
                actionMap["findNext"]?.resetDisable()
                actionMap["findPrevious"]?.resetDisable()
            }
        }
    }

    val currentNodes: List<ChapterNode>
        get() = with(tree.selectionModel) {
            val items = selectedItems.toList()
            clearSelection()
            items
        }

    val selectedNodes: ObservableList<ChapterNode> get() = tree.selectionModel.selectedItems

    val selectedNode get() = tree.selectionModel.selectedItem

    fun createChapter(): Chapter? {
        return input(tr("d.newChapter.title"), tr("d.newChapter.tip"), tr("jem.chapter.untitled"), false)?.let {
            Chapter(it)
        }
    }

    fun locateChapter(chapter: Chapter) {
        val node = locateNode(chapter, tree.root)
        if (node == null) {
            Log.d(TAG) { "Not found $chapter in nav" }
        } else {
            tree.selectAndScrollTo(node)
        }
    }

    @Command
    fun renameChapter() {
        val node = selectedNode
        val chapter = node!!.value
        input(tr("d.renameChapter.title"), tr("d.renameChapter.tip"), chapter.title, false, true)?.let {
            chapter.title = it
            node.refresh()
            EventBus.post(ModificationEvent(chapter, ModificationType.ATTRIBUTE_MODIFIED))
        }
    }

    @Command
    fun importChapter() {
        val files = openBookFiles() ?: return

        val targets = currentNodes
        val fxApp = Imabw.fxApp.apply { showProgress() }

        val succeed = AtomicInteger()
        val counter = AtomicInteger()

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
                insertNodes(listOf(task.value.toTreeItem()), targets, InsertMode.TO_PARENT)
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

    fun insertNodes(sources: Collection<ChapterNode>, targets: Collection<ChapterNode>, mode: InsertMode) {
        if (sources.isEmpty() || targets.isEmpty()) return
        require(sources.none { it in targets }) { "Cannot insert node to self" }
        val dump = targets.takeIf { it !== selectedNodes } ?: targets.toList()
        dump.forEachIndexed { index, target ->
            // clone chapter(s) except the first one
            val items = if (index == 0) sources else sources.map { it.value.clone().toTreeItem() }
            when (mode) {
                InsertMode.BEFORE_ITEM -> target.parent.let { insertNodes(items, it, it.children.indexOf(target)) }
                InsertMode.AFTER_ITEM -> target.parent.let { insertNodes(items, it, it.children.indexOf(target) + 1) }
                InsertMode.TO_PARENT -> insertNodes(items, target, -1)
            }
        }
    }

    fun insertNodes(sources: Collection<ChapterNode>, target: ChapterNode, index: Int) {
        if (sources.isEmpty()) return
        val model = tree.selectionModel
        val parent = target.value
        if (index < 0) {
            target.children += sources.onEach { parent += it.value }
        } else {
            target.children.addAll(index, sources)
            sources.reversed().forEach { parent.insert(index, it.value) }
        }
        sources.forEach { model.select(it) }
        Platform.runLater { tree.scrollTo(model.selectedIndices.last()) }
        EventBus.post(ModificationEvent(target.value, ModificationType.CONTENTS_MODIFIED))
    }

    fun removeNodes(nodes: Collection<ChapterNode>) {
        nodes.reversed().forEach {
            val chapter = it.value
            val parentNode = it.parent
            val parentChapter = parentNode.value
            parentNode.children.remove(it)
            parentChapter.remove(chapter)
            EditorPane.removeTab(chapter)
            Imabw.submit { chapter.cleanup() }
            EventBus.post(ModificationEvent(parentChapter, ModificationType.CONTENTS_MODIFIED))
        }
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
            node.isExpanded = false
        }
    }

    override fun handle(command: String, source: Any): Boolean {
        when (command) {
            "delete" -> removeNodes(selectedNodes)
            "selectAll" -> tree.selectionModel.selectAll()
            "editText" -> selectedNodes.forEach { EditorPane.openTab(it.value, CellFactory.getIcon(it)) }
            "newChapter" -> createChapter()?.let {
                insertNodes(listOf(it.toTreeItem()), currentNodes, InsertMode.TO_PARENT)
            }
            "insertChapter" -> createChapter()?.let {
                insertNodes(listOf(it.toTreeItem()), currentNodes, InsertMode.BEFORE_ITEM)
            }
            "exportChapter" -> Workbench.exportBooks(selectedNodes.map { it.value })
            "viewAttributes" -> if (editAttributes(selectedNode!!.value)) {
                EventBus.post(ModificationEvent(selectedNode!!.value, ModificationType.ATTRIBUTE_MODIFIED))
            }
            "bookAttributes" -> if (editAttributes(Workbench.work!!.book)) {
                EventBus.post(ModificationEvent(Workbench.work!!.book, ModificationType.ATTRIBUTE_MODIFIED))
            }
            "bookExtensions" -> Workbench.work!!.book.let {
                if (editVariants(it.extensions, tr("d.editExtension.title", it.title))) {
                    EventBus.post(ModificationEvent(it, ModificationType.EXTENSIONS_MODIFIED))
                }
            }
            "gotoChapter" -> EditorPane.selectedTab?.chapter?.let {
                locateChapter(it)
                tree.requestFocus()
            }
            "collapseToc" -> collapseNode(tree.root)
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
            Imabw.dashboard.designer.items["navTools"]?.let { items ->
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
        graphic = if (empty || chapter == null) null else CellFactory.getIcon(chapter)
        tooltip = null
        chapter?.intro?.let {
            with(LoadTextTask(it)) {
                setOnSucceeded {
                    value.takeIf { it.isNotEmpty() }?.let { text ->
                        tooltip = createTooltip(text)
                    }
                    hideProgress()
                }
                Imabw.submit(this)
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
