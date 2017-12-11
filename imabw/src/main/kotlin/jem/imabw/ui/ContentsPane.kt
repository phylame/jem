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

package jem.imabw.ui

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.util.Callback
import jclp.EventBus
import jclp.isRoot
import jclp.log.Log
import jclp.text.ifNotEmpty
import jem.Book
import jem.Chapter
import jem.epm.EXT_EPM_METADATA
import jem.epm.ParserParam
import jem.imabw.*
import jem.intro
import jem.title
import mala.App
import mala.App.tr
import mala.ixin.*
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger

private typealias ChapterNode = TreeItem<Chapter>

object ContentsPane : BorderPane(), CommandHandler {
    private const val TAG = "ContentsPane"

    private val treeView = object : TreeView<Chapter>(), EditAware {
        override fun onEdit(command: String) {
            handle(command, this)
        }
    }

    init {
        Imabw.register(this)

        id = "contents-pane"
        top = ContentsHeader
        center = treeView

        initTreeView()
        initActions()

        EventBus.register<WorkflowEvent> { event ->
            if (event.what == WorkflowType.BOOK_CREATED || event.what == WorkflowType.BOOK_OPENED) {
                treeView.root = event.book.toTreeItem()
                treeView.selectionModel.select(0)
                treeView.root.isExpanded = true
                treeView.requestFocus()
            }
        }
    }

    private fun initTreeView() {
        val tree = treeView
        tree.id = "contents-tree"
        tree.cellFactory = CellFactory
        tree.selectionModel.selectionMode = SelectionMode.MULTIPLE
        Imabw.dashboard.designer.items["navContext"]?.let { items ->
            tree.contextMenu = items.toContextMenu(Imabw, App, App.assets, IxIn.actionMap, null)
        }
        tree.setOnMouseClicked { event ->
            if (event.isDoubleClick && event.isPrimary) {
                currentNode?.takeIf { it.isLeaf && it.isNotRoot }?.let {
                    EditorPane.openTab(it.value, CellFactory.getIcon(it))
                    event.consume()
                }
            }
        }
        tree.setOnKeyPressed { event ->
            if (event.code == KeyCode.ENTER && !event.hasModifiers) {
                currentNode?.let {
                    if (!it.isLeaf) {
                        it.isExpanded = !it.isExpanded
                        event.consume()
                    } else if (it.isNotRoot) {
                        EditorPane.openTab(it.value, CellFactory.getIcon(it))
                        event.consume()
                    }
                }
            }
        }
    }

    private fun initActions() {
        val selection = currentNodes
        // FIXME why is null?
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
        actionMap["editText"]?.disableProperty?.bind(emptyOrBook)
        actionMap["moveChapter"]?.disableProperty?.bind(emptyOrBook)
        actionMap["mergeChapter"]?.disableProperty?.bind(emptyOrBook.or(multiple.not()))
        actionMap["upChapter"]?.disableProperty?.bind(emptyOrBook)
        actionMap["downChapter"]?.disableProperty?.bind(emptyOrBook)
        actionMap["upgradeChapter"]?.disableProperty?.bind(emptyOrBook)
        actionMap["degradeChapter"]?.disableProperty?.bind(emptyOrBook)
        actionMap["viewAttributes"]?.disableProperty?.bind(emptyOrMultiple)
        actionMap["gotoChapter"]?.disableProperty?.bind(Bindings.createBooleanBinding(Callable {
            EditorPane.selectionModel.selectedItem !is ChapterTab
        }, EditorPane.selectionModel.selectedItemProperty()))

        treeView.focusedProperty().addListener { _, _, focused ->
            if (focused) {
                val actions = arrayOf("undo", "redo", "cut", "copy", "paste", "replace")
                for (action in actions) {
                    actionMap[action]?.resetDisable(true)
                }
                actionMap["delete"]?.disableProperty?.bind(hasBook)
                actionMap["find"]?.resetDisable()
                actionMap["findNext"]?.resetDisable(true)
                actionMap["findPrevious"]?.resetDisable(true)
            }
        }
    }

    val currentNode: ChapterNode? get() = treeView.selectionModel.selectedItem

    val currentNodes: ObservableList<ChapterNode> get() = treeView.selectionModel.selectedItems

    fun createChapter(): Chapter? =
            input(tr("d.newChapter.title"), tr("d.newChapter.tip"), tr("jem.chapter.untitled"), canEmpty = false)?.let {
                Chapter(it)
            }

    fun locateChapter(chapter: Chapter) {
        val node = locateNode(chapter, treeView.root)
        if (node == null) {
            Log.d(TAG) { "not found $chapter in nav" }
        } else {
            treeView.selectAndScrollTo(node)
        }
    }

    @Command
    fun renameChapter() {
        val node = currentNode!!
        val chapter = node.value
        input(tr("d.renameChapter.title"), tr("d.renameChapter.tip"), chapter.title, canEmpty = false, mustDiff = true)?.let {
            chapter.title = it
            node.refresh()
            EventBus.post(ModificationEvent(chapter, ModificationType.ATTRIBUTE_MODIFIED))
        }
    }

    @Command
    fun importChapter() {
        val files = openBookFiles(Imabw.topWindow) ?: return

        val targets = treeView.resetSelection()
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
        val dump = targets.takeIf { it !== currentNodes } ?: targets.toList()
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
        val model = treeView.selectionModel
        val parent = target.value
        if (index < 0) {
            target.children += sources.onEach { parent += it.value }
        } else {
            target.children.addAll(index, sources)
            sources.reversed().forEach { parent.insert(index, it.value) }
        }
        sources.forEach { model.select(it) }
        Platform.runLater { treeView.scrollTo(model.selectedIndices.last()) }
        EventBus.post(ModificationEvent(target.value, ModificationType.CONTENTS_MODIFIED))
    }

    fun removeNodes(nodes: Collection<ChapterNode>) {
        val chapters = ArrayList<Chapter>(nodes.size)
        nodes.reversed().forEach { node ->
            val chapter = node.value
            val parentNode = node.parent
            val parentChapter = parentNode.value
            parentNode.children.remove(node)
            parentChapter.remove(chapter)
            EditorPane.removeTab(chapter)
            chapters.add(chapter)
            EventBus.post(ModificationEvent(parentChapter, ModificationType.CONTENTS_MODIFIED))
        }
        Imabw.submit {
            for (chapter in chapters) {
                chapter.cleanup()
            }
        }
    }

    fun locateNode(chapter: Chapter, node: ChapterNode): ChapterNode? {
        if (node.value === chapter) return node
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
            "delete" -> removeNodes(currentNodes)
            "selectAll" -> treeView.selectionModel.selectAll()
            "editText" -> currentNodes.forEach { EditorPane.openTab(it.value, CellFactory.getIcon(it)) }
            "newChapter" -> createChapter()?.let {
                insertNodes(listOf(it.toTreeItem()), treeView.resetSelection(), InsertMode.TO_PARENT)
            }
            "insertChapter" -> createChapter()?.let {
                insertNodes(listOf(it.toTreeItem()), treeView.resetSelection(), InsertMode.BEFORE_ITEM)
            }
            "exportChapter" -> Workbench.exportBooks(currentNodes.map { it.value })
            "viewAttributes" -> if (editAttributes(currentNode!!.value, Imabw.topWindow)) {
                EventBus.post(ModificationEvent(currentNode!!.value, ModificationType.ATTRIBUTE_MODIFIED))
            }
            "bookAttributes" -> if (editAttributes(Workbench.work!!.book, Imabw.topWindow)) {
                EventBus.post(ModificationEvent(Workbench.work!!.book, ModificationType.ATTRIBUTE_MODIFIED))
            }
            "bookExtensions" -> Workbench.work!!.book.let {
                if (editVariants(it.extensions, tr("d.editExtension.title", it.title), setOf(EXT_EPM_METADATA), Imabw.topWindow)) {
                    EventBus.post(ModificationEvent(it, ModificationType.EXTENSIONS_MODIFIED))
                }
            }
            "gotoChapter" -> EditorPane.currentTab?.chapter?.let {
                locateChapter(it)
                treeView.requestFocus()
            }
            "collapseToc" -> collapseNode(treeView.root)
            else -> return false
        }
        return true
    }
}

object ContentsHeader : BorderPane() {
    init {
        id = "contents-header"
        left = Label(tr("main.contents.title"), App.assets.graphicFor("tree/contents")).also { label ->
            label.id = "contents-title"
            BorderPane.setAlignment(label, Pos.CENTER)
        }

        right = ToolBar().also { toolBar ->
            toolBar.id = "contents-tool-bar"
            Imabw.dashboard.designer.items["navTools"]?.let { items ->
                toolBar.init(items, Imabw, App, App.assets, IxIn.actionMap)
            }
            BorderPane.setAlignment(toolBar, Pos.CENTER)
        }
    }
}

private object CellFactory : Callback<TreeView<Chapter>, TreeCell<Chapter>> {
    private val bookIcon = App.assets.imageFor("tree/book")!!
    private val sectionIcon = App.assets.imageFor("tree/section")!!
    private val chapterIcon = App.assets.imageFor("tree/chapter")!!

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
        if (chapter == null) {
            text = null
            graphic = null
            tooltip = null
        } else {
            text = chapter.title
            graphic = CellFactory.getIcon(chapter)
            chapter.intro?.let {
                with(LoadTextTask(it)) {
                    setOnSucceeded {
                        value.ifNotEmpty { tooltip = createTooltip(it) }
                        hideProgress()
                    }
                    Imabw.submit(this)
                }
            }
        }
    }

    private fun createTooltip(text: String) = Tooltip(text).also {
        it.isWrapText = true
        it.styleClass += "contents-intro-tooltip"
        it.maxWidthProperty().bind(widthProperty().multiply(1.2))
    }
}

enum class InsertMode {
    BEFORE_ITEM,
    AFTER_ITEM,
    TO_PARENT
}
