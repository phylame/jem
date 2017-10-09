package jem.imabw.toc

import javafx.beans.binding.Bindings
import javafx.collections.ListChangeListener
import javafx.concurrent.Task
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.util.Callback
import jem.Book
import jem.Chapter
import jem.epm.parseBook
import jem.imabw.Imabw
import jem.imabw.Work
import jem.imabw.Workbench
import jem.imabw.editor.ChapterTab
import jem.imabw.editor.EditorPane
import jem.imabw.ui.Editable
import jem.imabw.ui.editAttributes
import jem.imabw.ui.editVariants
import jem.imabw.ui.inputText
import jem.intro
import jem.title
import mala.App
import mala.App.tr
import mala.ixin.*

typealias ChapterNode = TreeItem<Chapter>

object NavPane : BorderPane(), CommandHandler, Editable {
    private val rootNode = ChapterNode()
    private val treeView = TreeView(rootNode)

    val isActive get() = treeView.isFocused

    val selectBooks get() = treeView.selectionModel.selectedItems.map { it.value as Book }

    // the root of selected items
    val singleTopBook = CommonBinding(treeView.selectionModel.selectedItems) {
        var count = 0
        var root: ChapterNode? = null
        it.forEach { item ->
            // todo why item is null?
            item?.mostBelow(rootNode)?.let {
                if (root !== it) ++count
                root = it
            }
        }
        if (count == 1) root else null
    }

    init {
        Imabw.register(this)

        id = "nav-pane"
        top = NavHeader
        center = treeView

        initTree()
        initActions()

        Workbench.tasks.addListener(ListChangeListener<Work> {
            while (it.next()) {
                if (it.wasAdded()) {
                    val start = rootNode.children.size
                    it.addedSubList.asSequence().map { createItem(it.book) }.also { items ->
                        rootNode.children += items
                        treeView.selectionModel.clearSelection()
                        treeView.selectionModel.selectRange(start, rootNode.children.size)
                    }
                } else if (it.wasRemoved()) {
                    val books = it.removed.map { it.book }
                    rootNode.children.removeIf { it.value in books }
                }
            }
        })
    }

    private fun initTree() {
        val tree = this.treeView
        tree.id = "toc-tree"
        tree.isShowRoot = false
        tree.cellFactory = CellFactory
        tree.selectionModel.selectionMode = SelectionMode.MULTIPLE
        tree.addEventHandler(MouseEvent.MOUSE_PRESSED) { event ->
            if (event.clickCount == 2 && event.isPrimaryButtonDown) {
                tree.selectionModel.selectedItem?.value?.takeIf { !it.isSection && it !is Book }?.let {
                    event.consume()
                    EditorPane.openText(it)
                }
            }
        }
        tree.addEventHandler(KeyEvent.KEY_PRESSED) { event ->
            // only ENTER pressed
            if (!event.isShortcutDown && !event.isShiftDown && !event.isAltDown && event.code == KeyCode.ENTER) {
                // todo why is null?
                tree.selectionModel.selectedItems
                        .filterNotNull()
                        .forEach {
                            if (it.children.isNotEmpty()) {
                                it.isExpanded = !it.isExpanded
                            } else if (it.parent !== rootNode) {
                                EditorPane.openText(it.value)
                            }
                        }
            }
        }
    }

    private fun initActions() {
        val selectedItems = treeView.selectionModel.selectedItems

        val bookCount = CommonBinding(selectedItems) { it.count { it.parent === rootNode } }
        val chapterCount = CommonBinding(selectedItems) { it.count { it.parent !== rootNode } }

        val hasBook = bookCount.isNotEqualTo(0)
        val hasChapter = chapterCount.isNotEqualTo(0)

        // enable when has selected item(s)
        val noSelection = Bindings.isEmpty(selectedItems)
        Imabw.getAction("newChapter")?.disableProperty?.bind(noSelection)
        Imabw.getAction("importChapter")?.disableProperty?.bind(noSelection)

        // enable when no book(s) selected
        val notOnlyChapter = noSelection.or(hasBook)
        Imabw.getAction("insertChapter")?.disableProperty?.bind(notOnlyChapter)
        Imabw.getAction("exportChapter")?.disableProperty?.bind(notOnlyChapter)

        val multiSelection = Bindings.size(selectedItems).greaterThan(1)
        val notSingleSelection = noSelection.or(multiSelection)

        // enable when only one item selected
        Imabw.getAction("renameChapter")?.disableProperty?.bind(notSingleSelection)

        Imabw.getAction("editText")?.disableProperty?.bind(noSelection)

        Imabw.getAction("moveChapter")?.disableProperty?.bind(noSelection)

        Imabw.getAction("mergeChapter")?.disableProperty?.bind(noSelection.or(multiSelection.not()))

        // same as rename chapter
        Imabw.getAction("viewAttributes")?.disableProperty?.bind(notSingleSelection)

        // enable when one root book is found
        val notSingleBook = singleTopBook.isNull
        Imabw.getAction("bookAttributes")?.disableProperty?.bind(notSingleBook)
        Imabw.getAction("bookExtensions")?.disableProperty?.bind(notSingleBook)

        // book actions, enable when only book(s) selected
        val notOnlyBook = noSelection.or(hasChapter)
        Imabw.getAction("closeFile")?.disableProperty?.bind(notOnlyBook)

        val singleBookModified = CommonBinding(selectedItems) {
            it.size == 1 && it.first().parent === rootNode && Workbench.isModified(it.first().value)
        }
        Imabw.getAction("saveFile")?.disableProperty?.bind(singleBookModified.isEqualTo(false))

        Imabw.getAction("saveAsFile")?.disableProperty?.bind(notOnlyBook)

        Imabw.getAction("fileDetails")?.disableProperty?.bind(bookCount.isNotEqualTo(1).or(hasChapter))
    }

    // chapters will be clone for multi-target
    fun insertItems(sources: Collection<ChapterNode>, targets: Collection<ChapterNode>, mode: ItemInsertMode) {
        val backups = targets.toTypedArray() // targets may be selectedItems
        val selectionModel = treeView.selectionModel.apply { clearSelection() }
        backups.forEachIndexed { i, target ->
            // clone chapter(s) except the first one
            val items = if (i == 0) sources else sources.map { createItem(it.value.clone()) }.toList()
            when (mode) {
                ItemInsertMode.BEFORE_ITEM -> target.parent.let { insertItems(items, it, it.children.indexOf(target)) }
                ItemInsertMode.AFTER_ITEM -> target.parent.let { insertItems(items, it, it.children.indexOf(target) + 1) }
                ItemInsertMode.TO_PARENT -> insertItems(items, target, -1)
            }
            items.forEach(selectionModel::select)
        }
    }

    fun insertItems(sources: Collection<ChapterNode>, target: ChapterNode, index: Int) {
        val children = target.children // children is lazied
        if (index < 0) {
            children += sources
        } else {
            children.addAll(index, sources)
        }
    }

    fun removeItems(sources: Collection<ChapterNode>) {
        sources.reversed().forEach {
            println("to remove ${it.value}")
            it.parent.children.remove(it)
            it.value.cleanup()
        }
        // todo why not clear selections
        if (treeView.selectionModel.selectedItems.firstOrNull() == null) { // removed all books
            treeView.selectionModel.clearSelection()
        }
    }

    fun createItem(chapter: Chapter) = chapter.toTreeItem().also { item ->
        item.children.addListener(ListChangeListener {
            val parent = item.value
            while (it.next()) {
                if (it.wasAdded()) {
                    it.list.subList(it.from, it.to).forEach {
                        println("append '${it.value.title}' to '${parent.title}'")
                        parent.append(it.value)
                    }
                }
                if (it.wasRemoved()) {
                    it.removed.forEach {
                        println("remove '${it.value.title}' from '${parent.title}'")
                        parent.remove(it.value)
                    }
                }
            }
        })
    }

    inline fun createChapter(block: (Chapter) -> Unit) {
        inputText(tr("d.newChapter.title"), tr("d.newChapter.tip"), tr("chapter.untitled")) {
            block(Chapter(it))
        }
    }

    @Command
    fun renameChapter() {
        val treeItem = treeView.selectionModel.selectedItem
        val chapter = treeItem!!.value
        inputText(tr("d.renameChapter.title"), tr("d.renameChapter.tip"), chapter.title) {
            chapter.title = it
            treeItem.refresh()
        }
    }

    @Command
    fun gotoChapter() {
        (EditorPane.selectionModel.selectedItem as? ChapterTab)?.chapter?.let {
            locateItem(it, rootNode)?.let {
                treeView.selectionModel.clearSelection()
                treeView.selectionModel.select(it)
                treeView.scrollTo(treeView.selectionModel.selectedIndex)
            }
        }
    }

    @Command
    fun importChapter() {
        val task = object : Task<Book>() {
            override fun call() = parseBook("E:/tmp/2", "pmab")
        }
        task.setOnSucceeded {
            insertItems(listOf(createItem(task.value)), treeView.selectionModel.selectedItems, ItemInsertMode.TO_PARENT)
        }
        task.setOnFailed {
            task.exception.printStackTrace()
        }
        Imabw.submit(task)
    }

    private fun locateItem(chapter: Chapter, from: ChapterNode): ChapterNode? {
        if (from.value === chapter) {
            return from
        }
        for (item in from.children) {
            locateItem(chapter, item)?.let { return it }
        }
        return null
    }

    private fun collapseNode(node: ChapterNode) {
        if (node.isExpanded) {
            node.children.forEach(this::collapseNode)
        }
        node.isExpanded = false
    }

    override fun handle(command: String, source: Any): Boolean {
        when (command) {
            "collapseToc" -> {
                val selectedBooks = treeView.selectionModel.selectedItems.filter { it.parent === rootNode }
                if (selectedBooks.size != 1) {
                    treeView.selectionModel.clearSelection()
                }
                rootNode.children.forEach(this::collapseNode)
                if (selectedBooks.size != 1) {
                    treeView.selectionModel.selectFirst()
                }
            }
            "editText" -> treeView.selectionModel.selectedItems.forEach { EditorPane.openText(it.value) }
            "newChapter" -> createChapter {
                insertItems(listOf(createItem(it)), treeView.selectionModel.selectedItems, ItemInsertMode.TO_PARENT)
            }
            "insertChapter" -> createChapter {
                insertItems(listOf(createItem(it)), treeView.selectionModel.selectedItems, ItemInsertMode.BEFORE_ITEM)
            }
            "exportChapter" -> Workbench.exportBook(treeView.selectionModel.selectedItems.map { it.value })
            "viewAttributes" -> editAttributes(treeView.selectionModel.selectedItem.value)
            "bookAttributes" -> editAttributes(singleTopBook.value!!.value)
            "bookExtensions" -> singleTopBook.value!!.value.let {
                editVariants((it as Book).extensions, tr("d.editExtension.title", it.title))
            }
            else -> return false
        }
        return true
    }

    override fun onEdit(command: String) {
        when (command) {
            "delete" -> removeItems(treeView.selectionModel.selectedItems)
            "selectAll" -> treeView.selectionModel.selectAll()
            else -> TODO()
        }
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
            Imabw.form.appDesigner.items["navTools"]?.let { items ->
                it.init(items, Imabw, App, App.assets, Imabw.actionMap)
            }
            initActions()
            BorderPane.setAlignment(it, Pos.CENTER)
        }
    }

    private fun initActions() {
        val notChapterTab = CommonBinding(EditorPane.selectionModel.selectedItemProperty()) { it.value !is ChapterTab }
        Imabw.getAction("gotoChapter")?.disableProperty?.bind(notChapterTab)
    }
}

object CellFactory : Callback<TreeView<Chapter>, TreeCell<Chapter>> {
    val book = App.assets.imageFor("tree/book")
    val section = App.assets.imageFor("tree/section")
    val chapter = App.assets.imageFor("tree/chapter")

    override fun call(param: TreeView<Chapter>): TreeCell<Chapter> {
        return ChapterCell()
    }
}

private class ChapterCell : TreeCell<Chapter>() {
    private val menu = ContextMenu()

    init {
        Imabw.form.appDesigner.items["navContext"]?.let {
            menu.init(it, Imabw, App, App.assets, Imabw.actionMap, null)
        }
    }

    override fun updateItem(chapter: Chapter?, empty: Boolean) {
        super.updateItem(chapter, empty)
        text = chapter?.title
        graphic = when {
            empty || chapter == null -> null
            chapter is Book && chapter.parent == null -> ImageView(CellFactory.book)
            chapter.isSection -> ImageView(CellFactory.section)
            else -> ImageView(CellFactory.chapter)
        }
        tooltip = chapter?.intro?.toString()?.takeIf(String::isNotEmpty)?.let { intro ->
            Tooltip(intro).also {
                it.isWrapText = true
                it.styleClass += "intro-tooltip"
                it.maxWidthProperty().bind(widthProperty())
            }
        }
        contextMenu = chapter?.let { menu }
    }
}


enum class ItemInsertMode {
    BEFORE_ITEM,
    AFTER_ITEM,
    TO_PARENT
}
