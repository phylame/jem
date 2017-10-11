package jem.imabw.toc

import javafx.beans.binding.Bindings
import javafx.collections.ListChangeListener
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
import jclp.isNotRoot
import jclp.log.Log
import jem.Book
import jem.Chapter
import jem.epm.parseBook
import jem.imabw.Imabw
import jem.imabw.Workbench
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
import java.util.concurrent.Callable

typealias ChapterNode = TreeItem<Chapter>

object NavPane : BorderPane(), CommandHandler, Editable {
    private const val TAG = "Nav"

    val treeView = TreeView(ChapterNode())

    val isActive get() = treeView.isFocused

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
        }
    }

    private fun initTree() {
        val tree = this.treeView
        tree.id = "toc-tree"
        tree.cellFactory = CellFactory
        tree.selectionModel.selectionMode = SelectionMode.MULTIPLE
        tree.addEventHandler(MouseEvent.MOUSE_PRESSED) { event ->
            if (event.clickCount == 2 && event.isPrimaryButtonDown) {
                selectedChapter?.takeIf { !it.isSection && it.isNotRoot }?.let {
                    EditorPane.openText(it)
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
                        EditorPane.openText(it.value)
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

        val actionMap = Imabw.actionMap
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
            scene.focusOwnerProperty().addListener { _, _, new ->
                val actions = arrayOf("undo", "redo", "cut", "copy", "paste", "find", "findNext", "findPrevious")
                if (new === treeView) {
                    for (action in actions) {
                        actionMap[action]?.isDisable = true
                    }
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
            Workbench.work.isModified = true
        }
    }

    @Command
    fun importChapter() {
        val task = object : Task<Book>() {
            override fun call() = parseBook("E:/tmp/2", "pmab")
        }
        task.setOnSucceeded {
            insertNodes(listOf(createNode(task.value)), selection, ItemMode.TO_PARENT)
            println("inserted 1 book")
        }
        task.setOnFailed {
            task.exception.printStackTrace()
        }
        Imabw.submit(task)
    }

    fun createNode(chapter: Chapter) = chapter.toTreeItem().apply {
        val parent = value
        children.addListener(ListChangeListener {
            while (it.next()) {
                it.addedSubList.forEach { parent.append(it.value) }
                it.removed.forEach { parent.remove(it.value) }
            }
        })
    }

    fun insertNodes(sources: Collection<ChapterNode>, targets: Collection<ChapterNode>, mode: ItemMode) {
        if (sources.isEmpty() || targets.isEmpty()) return
        require(sources.none { it in targets }) { "Cannot insert node to self" }
        val dump = targets.takeIf { it !== selection } ?: targets.toList()
        val model = treeView.selectionModel.apply { clearSelection() }
        dump.forEachIndexed { index, target ->
            // clone chapter(s) except the first one
            val items = if (index == 0) sources else sources.map { createNode(it.value.clone()) }
            when (mode) {
                ItemMode.BEFORE_ITEM -> target.parent.let { insertNodes(items, it, it.children.indexOf(target)) }
                ItemMode.AFTER_ITEM -> target.parent.let { insertNodes(items, it, it.children.indexOf(target) + 1) }
                ItemMode.TO_PARENT -> insertNodes(items, target, -1)
            }
            items.forEach { model.select(it) }
        }
        Workbench.work.isModified = true
    }

    fun insertNodes(sources: Collection<ChapterNode>, target: ChapterNode, index: Int) {
        if (sources.isEmpty()) return
        if (index < 0) {
            target.children += sources
        } else {
            target.children.addAll(index, sources)
        }
    }

    fun removeNodes(nodes: Collection<ChapterNode>) {
        nodes.reversed().forEach {
            it.parent.children.remove(it)
            it.value.let {
                EditorPane.closeText(it)
                Imabw.submit { it.cleanup() }
            }
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
            "editText" -> selection.forEach { EditorPane.openText(it.value) }
            "newChapter" -> createChapter()?.let {
                insertNodes(listOf(createNode(it)), selection, ItemMode.TO_PARENT)
            }
            "insertChapter" -> createChapter()?.let {
                insertNodes(listOf(createNode(it)), selection, ItemMode.BEFORE_ITEM)
            }
            "exportChapter" -> Workbench.exportBook(selection.map { it.value })
            "viewAttributes" -> editAttributes(selectedChapter!!)
            "bookAttributes" -> editAttributes(Workbench.work.book)
            "bookExtensions" -> Workbench.work.book.let {
                editVariants(it.extensions, tr("d.editExtension.title", it.title))
            }
            "gotoChapter" -> EditorPane.selectedTab?.chapter?.let(this::locateChapter)
            "collapseToc" -> collapseNode(treeView.root)
            else -> return false
        }
        return true
    }

    override fun onEdit(command: String) {
        when (command) {
            "delete" -> removeNodes(selection)
            "selectAll" -> treeView.selectionModel.selectAll()
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
            BorderPane.setAlignment(it, Pos.CENTER)
        }
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


enum class ItemMode {
    BEFORE_ITEM,
    AFTER_ITEM,
    TO_PARENT
}
