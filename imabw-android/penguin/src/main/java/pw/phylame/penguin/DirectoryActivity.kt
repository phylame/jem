package pw.phylame.penguin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import pw.phylame.ancotols.*
import java.io.File
import java.io.FileFilter
import java.util.*
import java.util.regex.Pattern

internal class DirectoryActivity : BaseActivity(), View.OnClickListener {
    companion object {
        const val DEVICE_NAME_KEY = "_device_name_key"
    }

    val pattern: String? by lazy { intent.getStringExtra(PenguinActivity.PATTERN_KEY) }

    val initPath: String by lazy { intent.getStringExtra(PenguinActivity.INIT_PATH_KEY) }

    val isDirMode: Boolean by lazy { intent.getBooleanExtra(PenguinActivity.DIR_MODE_KEY, false) }

    val isMultiple: Boolean by lazy { intent.getBooleanExtra(PenguinActivity.MULTIPLE_KEY, false) }

    val pathBar: ViewGroup by lazyView(R.id.path_bar)

    val progressBar: ProgressBar by lazyView(R.id.progress_bar)

    val selectionInfo: TextView by lazyView(R.id.info)

    val btnOk: Button by lazyView(R.id.btn_ok)

    val btnSelectAll: ImageButton by lazyView(R.id.btn_select_all)

    val emptyTip: View by lazyView(R.id.tip)

    lateinit var directory: Directory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_directory)
        setSupportActionBar(this[R.id.toolbar])

        val titleId = if (isDirMode) R.plurals.dir_mode_title else R.plurals.file_mode_title
        title = resources.getQuantityString(titleId, if (isMultiple) 2 else 1)

        btnSelectAll.setOnClickListener(this)
        btnSelectAll.visibility = if (isMultiple) View.VISIBLE else View.GONE
        selectionInfo.visibility = btnSelectAll.visibility
        btnOk.setOnClickListener(this)
        btnOk.visibility = if (isDirMode || isMultiple) View.VISIBLE else View.GONE
        findViewById(R.id.btn_cancel).setOnClickListener(this)

        Filter.isDirMode = isDirMode
        Filter.pattern = pattern?.replace(".", "\\.")?.replace("*", ".*")?.toPattern()

        directory = Directory(this[R.id.list])
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_directory, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var needRefresh = true
        when (item.itemId) {
            R.id.action_hidden -> Filter.isShowHidden = !Filter.isShowHidden
            R.id.action_by_name -> Sorter.sortType = Sorter.BY_NAME
            R.id.action_by_type -> Sorter.sortType = Sorter.BY_TYPE
            R.id.action_by_size -> Sorter.sortType = Sorter.BY_SIZE_ASC
            R.id.action_by_size_desc -> Sorter.sortType = Sorter.BY_SIZE_DESC
            R.id.action_by_date -> Sorter.sortType = Sorter.BY_DATE_ASC
            R.id.action_by_date_desc -> Sorter.sortType = Sorter.BY_DATE_DESC
            R.id.action_new -> {
                needRefresh = false
            }
            else -> return super.onOptionsItemSelected(item)
        }
        if (needRefresh) {
            directory.refresh(true)
        }
        return true
    }

    override fun onBackPressed() {
        if (!directory.backParent()) {
            super.onBackPressed()
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_cancel -> finish()
            R.id.btn_ok -> {
                if (isMultiple) {
                    finish(directory.getSelections().toTypedArray())
                } else if (isDirMode) {
                    finish(arrayOf(directory.item))
                }
            }
            R.id.btn_select_all -> {
                directory.toggleSelections()
                btnSelectAll.isSelected = !btnSelectAll.isSelected
                btnSelectAll.setImageResource(Adapter.selectionIcon(btnSelectAll.isSelected))
            }
        }
    }

    fun finish(items: Array<Item>) {
        val data = Intent()
        if (items.size == 1) {
            data.putExtra(PenguinActivity.RESULT_KEY, items.first().file.path)
        } else {
            data.putExtra(PenguinActivity.RESULT_KEY, items.joinToString(",") { it.file.path })
        }
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    inner class Directory(list: ListView) : ListNGModel<Item>() {
        init {
            Item.rootName = intent.getStringExtra(DEVICE_NAME_KEY)
            Item.root = File(initPath)
            isAsyncMode = true
            this.isGroupMode = isDirMode
            this.isMultiple = isMultiple
            setup(list, Adapter(this@DirectoryActivity, isDirMode, isMultiple))
            activateItem(Item(Item.root))
        }

        private var isProgressShown = false

        override fun onPreFetch() {
            if (item.count < 0 || item.count > 120) {
                isProgressShown = true
                showProgress(this@DirectoryActivity, progressBar, true)
            }
        }

        override fun onFetchItems(parent: Item): List<Item> {
            val items = ArrayList<Item>(if (parent.count < 0) 16 else parent.count)
            var dirCount = 0
            var fileCount = 0
            IOs.itemsOf(parent.file, Filter).asSequence().sortedWith(Sorter).forEach {
                if (it.isFile) {
                    ++fileCount
                }
                if (it.isDirectory) {
                    ++dirCount
                }
                val item = Item(it)
                item.count = IOs.countOf(it, Filter)
                items.add(item)
            }
            parent.count = items.size
            parent.dirCount = dirCount
            parent.fileCount = fileCount
            return items
        }

        private val itemSize by lazyDimen(R.dimen.path_bar_text_size)

        private val itemColor by lazyColor(R.color.pathText)

        override fun onPostFetch() {
            if (adapter.count > 0) {
                emptyTip.visibility = View.GONE
            } else {
                emptyTip.visibility = View.VISIBLE
            }
            refreshPathBar(pathBar, itemSize, itemColor)
            val size = getSelections().size
            selectionInfo.text = resources.getQuantityString(R.plurals.selection_info, size, size)
            if (isProgressShown) {
                isProgressShown = false
                showProgress(this@DirectoryActivity, progressBar, false)
            }
        }

        override fun onItemToggled(item: Item) {
            val holder = item.view?.tag as Holder?
            if (holder != null) {
                holder.icon2.setImageResource(Adapter.selectionIcon(item.isSelected))
            }
        }

        override fun onItemClicked(item: Item, view: View, position: Int, id: Long) {
            finish(arrayOf(item))
        }
    }
}

internal object Sorter : Comparator<File> {
    const val BY_NAME = 1
    const val BY_TYPE = 2
    const val BY_DATE_ASC = 3
    const val BY_DATE_DESC = 4
    const val BY_SIZE_ASC = 5
    const val BY_SIZE_DESC = 6

    private const val FRONTAL = -1
    private const val POSTERIOR = 1

    /**
     * Show directories before files
     */
    var isDirFirst = true

    /**
     * Order for hidden items
     */
    var hiddenOrder = FRONTAL

    /**
     * Sort type for files.
     */
    var sortType = BY_NAME

    override fun compare(a: File, b: File): Int {
        val dirA = a.isDirectory
        val dirB = b.isDirectory
        if (isDirFirst) {
            if (dirA) {
                if (!dirB) {
                    return FRONTAL
                }
            } else if (dirB) {
                return POSTERIOR
            }
        }

        val nameA = a.name
        val nameB = b.name

        if (dirA && dirB) { // directories only sorted by name
            return byName(nameA, nameB, a, b, FRONTAL)
        }

        return when (sortType) {
            BY_NAME -> byName(nameA, nameB, a, b, hiddenOrder)
            BY_TYPE -> byType(nameA, nameB, a.extension, b.extension, a, b, hiddenOrder)
            BY_DATE_ASC -> byNumber(a.lastModified(), b.lastModified(), true, nameA, nameB, a, b, hiddenOrder)
            BY_DATE_DESC -> byNumber(a.lastModified(), b.lastModified(), false, nameA, nameB, a, b, hiddenOrder)
            BY_SIZE_ASC -> byNumber(a.length(), b.length(), true, nameA, nameB, a, b, hiddenOrder)
            BY_SIZE_DESC -> byNumber(a.length(), b.length(), false, nameA, nameB, a, b, hiddenOrder)
            else -> nameA.compareTo(nameB, true)
        }
    }

    private fun byHidden(a: File, b: File, hidden: Int): Int {
        if (hidden != 0) {
            if (a.isHidden) {
                if (!b.isHidden) {
                    return hidden
                }
            } else if (b.isHidden) {
                return -hidden
            }
        }
        return 0
    }

    private fun byName(nameA: String, nameB: String, a: File, b: File, hidden: Int): Int {
        val order = byHidden(a, b, hidden)
        return if (order != 0) order else nameA.compareTo(nameB, ignoreCase = true)
    }

    private fun byType(nameA: String, nameB: String, extA: String, extB: String, a: File, b: File, hidden: Int): Int {
        var order = byHidden(a, b, hidden)
        if (order != 0) {
            return order
        }
        order = extA.compareTo(extB)
        return if (order != 0) order else nameA.compareTo(nameB, ignoreCase = true)
    }

    private fun byNumber(numA: Long, numB: Long, asc: Boolean, nameA: String, nameB: String, a: File, b: File, hidden: Int): Int {
        var order = byHidden(a, b, hidden)
        if (order != 0) {
            return order
        }
        if (numA == numB) {
            return nameA.compareTo(nameB, ignoreCase = true)
        }
        order = if (numA < numB) FRONTAL else POSTERIOR
        return if (asc) order else -order
    }
}

internal object Filter : FileFilter {
    var pattern: Pattern? = null
    var isDirMode: Boolean = false
    var isShowHidden: Boolean = false

    override fun accept(file: File): Boolean {
        if (!isShowHidden && file.isHidden) {
            return false
        }
        if (isDirMode && !file.isDirectory) {
            return false
        }
        if (file.isDirectory) {
            return true
        }
        if (pattern != null && file.isFile) {
            return pattern!!.matcher(file.name).matches()
        }
        return true
    }
}

internal class Item(val file: File) : ListNGItem() {
    companion object {
        var rootName = ""
        lateinit var root: File
    }

    var count = -1

    var dirCount = 0

    var fileCount = 0

    override val parent: ListNGItem? get() {
        if (isRoot) {
            return null
        } else {
            return Item(file.parentFile ?: return null)
        }
    }

    override val isRoot: Boolean get() = file == root

    override val isGroup: Boolean get() = file.isDirectory

    override val name: CharSequence get() = if (isRoot) rootName else file.name

    override fun toString(): String = "Item(file=$file, count=$count, dirCount=$dirCount, fileCount=$fileCount)"

    override fun hashCode(): Int = file.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        return when (other) {
            !is Item -> false
            else -> file == other.file
        }
    }
}

internal class Holder(view: View) {
    val icon1: ImageView = view[R.id.icon1]
    val text1: TextView = view[R.id.text1]
    val text2: TextView = view[R.id.text2]
    val icon2: ImageView = view[R.id.icon2]
}

internal class Adapter(val context: Context, val isDirMode: Boolean, val isMultiple: Boolean) :
        ListNGAdapter<Item>() {
    companion object {
        fun folderIcon(count: Int): Int = if (count == 0)
            R.drawable.ic_folder_empty
        else
            R.drawable.ic_folder

        fun selectionIcon(state: Boolean): Int = if (state)
            R.drawable.ic_checkbox_selected
        else
            R.drawable.ic_checkbox_normal
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val (view, holder) = reusedView(convertView, context, R.layout.icon_list_item, ::Holder)
        val item = getItem(position)
        refreshView(item, holder)
        item.view = view
        return view
    }

    fun refreshView(item: Item, holder: Holder) {
        val file = item.file
        val date = Date(file.lastModified())
        if (file.isDirectory) {
            val count = item.count
            holder.icon1.setImageResource(folderIcon(count))
            holder.text2.text = context.resources.getQuantityString(R.plurals.directory_info, count, count, date)
            holder.icon2.visibility = View.VISIBLE
            if (isDirMode && isMultiple) {
                holder.icon2.setImageResource(selectionIcon(item.isSelected))
            } else {
                holder.icon2.setImageResource(R.drawable.ic_arrow)
            }
        } else {
            holder.icon1.setImageResource(R.drawable.ic_file)
            holder.text2.text = context.getString(R.string.file_info, IOs.readable(file.length()), date)
            if (isMultiple) {
                holder.icon2.visibility = View.VISIBLE
                holder.icon2.setImageResource(selectionIcon(item.isSelected))
            } else {
                holder.icon2.visibility = View.GONE
            }
        }
        holder.text1.text = file.name
    }

}
