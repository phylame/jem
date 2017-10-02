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
import jclp.root
import jem.Book
import jem.Chapter
import jem.epm.parseBook
import jem.imabw.Imabw
import jem.imabw.Work
import jem.imabw.Workbench
import jem.imabw.editor.ChapterTab
import jem.imabw.editor.EditorPane
import jem.imabw.ui.EditableComponent
import jem.intro
import jem.title
import mala.App
import mala.App.tr
import mala.ixin.*
import java.util.concurrent.ThreadLocalRandom

typealias ChapterNode = TreeItem<Chapter>

object NavPane : BorderPane(), CommandHandler, EditableComponent {
    private val root = ChapterNode()
    private val tree = TreeView(root)

    val isActive get() = tree.isFocused

    // the root of selected items
    val rootNodeBinding = CommonBinding(tree.selectionModel.selectedItems) {
        var count = 0
        var rootNode: ChapterNode? = null
        var book: Chapter? = null
        for (item in it) {
            // todo why is null?
            item?.value?.root?.also {
                if (book !== it) ++count
                rootNode = item
                book = it
            }
        }
        if (count == 1) rootNode else null
    }

    init {
        Imabw.register(this)

        id = "nav-pane"
        top = NavHeader
        center = tree

        initTree()
        initActions()

        Workbench.tasks.addListener(ListChangeListener<Work> {
            while (it.next()) {
                if (it.wasAdded()) {
                    val start = root.children.size
                    it.addedSubList.asSequence().map { createItem(it.book) }.also { items ->
                        root.children += items
                        tree.selectionModel.clearSelection()
                        tree.selectionModel.selectRange(start, root.children.size)
                    }
                } else if (it.wasRemoved()) {
                    val books = it.removed.map { it.book }
                    root.children.removeIf { it.value in books }
                }
            }
        })
    }

    private fun initTree() {
        val tree = this.tree
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
                            } else if (it.parent !== root) {
                                EditorPane.openText(it.value)
                            }
                        }
            }
        }
    }

    private fun initActions() {
        val selectedItems = tree.selectionModel.selectedItems

        val bookCount = CommonBinding(selectedItems) { it.count { it.parent === root } }
        val chapterCount = CommonBinding(selectedItems) { it.count { it.parent !== root } }

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
        val notSingleBook = rootNodeBinding.isNull
        Imabw.getAction("bookAttributes")?.disableProperty?.bind(notSingleBook)
        Imabw.getAction("bookExtensions")?.disableProperty?.bind(notSingleBook)

        // book actions, enable when only book(s) selected
        val notOnlyBook = noSelection.or(hasChapter)
        Imabw.getAction("closeFile")?.disableProperty?.bind(notOnlyBook)
        Imabw.getAction("saveFile")?.disableProperty?.bind(notOnlyBook)
        Imabw.getAction("saveAsFile")?.disableProperty?.bind(notOnlyBook)
        Imabw.getAction("duplicateFile")?.disableProperty?.bind(notOnlyBook)
        Imabw.getAction("lockFile")?.disableProperty?.bind(notOnlyBook)

        Imabw.getAction("fileDetails")?.disableProperty?.bind(bookCount.isNotEqualTo(1).or(hasChapter))
    }

    inline fun createChapter(block: (Chapter) -> Unit) {
        val chapter = Chapter("Chapter ${ThreadLocalRandom.current().nextInt(36)}")
        block(chapter)
    }

    // chapters will be clone for multi-target
    fun insertItems(sources: Collection<ChapterNode>, targets: Collection<ChapterNode>, mode: ItemInsertMode) {
        val backups = targets.toTypedArray() // targets may be selectedItems
        val selectionModel = tree.selectionModel.apply { clearSelection() }
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
        if (tree.selectionModel.selectedItems.firstOrNull() == null) { // removed all books
            tree.selectionModel.clearSelection()
        }
    }

    fun createItem(chapter: Chapter) = chapter.toTreeItem().also { item ->
        item.children.addListener(ListChangeListener {
            val parent = item.value
            while (it.next()) {
                if (it.wasAdded()) {
                    it.list.subList(it.from, it.to).forEach { parent.append(it.value) }
                }
                if (it.wasRemoved()) {
                    it.removed.forEach { parent.remove(it.value) }
                }
            }
        })
    }

    @Command
    fun gotoChapter() {
        (EditorPane.selectionModel.selectedItem as? ChapterTab)?.chapter?.let {
            locateItem(it, root)?.let {
                tree.selectionModel.clearSelection()
                tree.selectionModel.select(it)
            }
        }
    }

    @Command
    fun importChapter() {
        val task = object : Task<Book>() {
            override fun call() = parseBook("E:/tmp/2", "pmab")
        }
        task.setOnSucceeded {
            insertItems(listOf(createItem(task.value)), tree.selectionModel.selectedItems, ItemInsertMode.TO_PARENT)
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
                val selectedBooks = tree.selectionModel.selectedItems.filter { it.parent === root }
                if (selectedBooks.size != 1) {
                    tree.selectionModel.clearSelection()
                }
                root.children.forEach(this::collapseNode)
                if (selectedBooks.size != 1) {
                    tree.selectionModel.selectFirst()
                }
            }
            "editText" -> tree.selectionModel.selectedItems.forEach { EditorPane.openText(it.value) }
            "newChapter" -> createChapter {
                insertItems(listOf(createItem(it)), tree.selectionModel.selectedItems, ItemInsertMode.TO_PARENT)
            }
            "insertChapter" -> createChapter {
                insertItems(listOf(createItem(it)), tree.selectionModel.selectedItems, ItemInsertMode.BEFORE_ITEM)
            }
            else -> return false
        }
        return true
    }

    override fun editCommand(command: String) {
        when (command) {
            "delete" -> removeItems(tree.selectionModel.selectedItems)
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
