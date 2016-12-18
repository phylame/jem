package pw.phylame.imabw.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v7.widget.Toolbar
import android.util.Log
import android.util.Pair
import android.view.*
import android.widget.*
import pw.phylame.android.listng.AbstractItem
import pw.phylame.android.listng.Adapter
import pw.phylame.android.listng.Item
import pw.phylame.android.listng.ListSupport
import pw.phylame.android.util.BaseActivity
import pw.phylame.android.util.UIs
import pw.phylame.imabw.R
import pw.phylame.jem.*
import pw.phylame.jem.core.Book
import pw.phylame.jem.core.Chapter
import pw.phylame.jem.epm.EpmManager
import pw.phylame.seal.SealActivity
import rx.Observable
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File
import java.util.*

class BookActivity : BaseActivity() {
    companion object {
        private val TAG = BookActivity::class.java.simpleName
        private const val REQUEST_OPEN = 100
        private const val REQUEST_TEXT = 101
        private const val REQUEST_PROPERTIES = 102
    }

    private lateinit var bookList: BookList

    private lateinit var toolbar: Toolbar
    private lateinit var pathBarHolder: HorizontalScrollView
    private lateinit var pathBar: LinearLayout
    private lateinit var detailBar: TextView
    private lateinit var searchView: SearchView

    private val pathBarButtonFontSize: Float by lazy {
        UIs.dimenFor(resources, R.dimen.book_path_font_size)
    }

    private val pathBarButtonPadding: Int by lazy {
        resources.getDimensionPixelSize(R.dimen.book_path_margin)
    }

    private var progressBar: ProgressBar? = null

    override fun onStart() {
        System.setProperty(EpmManager.AUTO_LOAD_CUSTOMIZED_KEY, "true")
        super.onStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book)
        toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        pathBarHolder = findViewById(R.id.sv_path_bar) as HorizontalScrollView
        pathBar = findViewById(R.id.path_bar) as LinearLayout
        detailBar = findViewById(R.id.section_detail) as TextView

        val listView = findViewById(R.id.list) as ListView
        registerForContextMenu(listView)

        findViewById(R.id.fab).setOnClickListener { addChapter(bookList.current) }

        bookList = BookList(listView)
        newBook()

        println(Environment.getExternalStorageDirectory())
    }

    fun chooseFile() {
        SealActivity.choose(this, REQUEST_OPEN, true, false, false, false, null, Task.path)
    }

    private fun showProgress(shown: Boolean) {
        if (progressBar == null) {
            progressBar = findViewById(R.id.progress_bar) as ProgressBar
        }
        UIs.showProgress(this, progressBar, shown)
    }

    fun newBook(title: String? = null) {
        Observable.create<Book> {
            Task.cleanup()
            it.onNext(makeSimpleBook(title))
        }.subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).subscribe {
            bookList.current = ChapterItem(it)
            Task.book = it
        }
    }

    private fun makeSimpleBook(title: String?): Book {
        val book = Book(title ?: getString(R.string.book_default_book_name))
        book.author = ""
        book.pubdate = Date()
        addAttributes(book)
        return book
    }

    fun openBook(file: File, format: String? = null, arguments: Map<String, Any>? = null) {
        showProgress(true)
        toolbar.setTitle(R.string.book_open_progress)
        toolbar.subtitle = null

        Observable.create<Book> {
            Task.cleanup()
            it.onNext(EpmManager.readBook(file, format ?: EpmManager.formatOfFile(file.path), arguments))
            it.onCompleted()
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Subscriber<Book>() {
                    override fun onError(e: Throwable) {
                        Log.e(TAG, "failed to load book", e)
                        showProgress(false)
                        refreshTitle()
                        UIs.alert(this@BookActivity, getString(R.string.book_open_book_failed), e.message)
                    }

                    override fun onNext(book: Book) {
                        bookList.current = ChapterItem(book)
                        Task.path = file.path
                        Task.book = book
                    }

                    override fun onCompleted() {
                        showProgress(false)
                    }
                })
    }

    private fun addChapter(item: ChapterItem) {

    }

    private fun insertChapter(item: ChapterItem, position: Int) {

    }

    private fun renameChapter(item: ChapterItem) {

    }

    private fun removeChapter(item: ChapterItem, position: Int) {

    }

    private fun refreshTitle() {
        val book = Task.book
        if (book != null) {
            toolbar.title = book.title
            toolbar.subtitle = getString(R.string.book_detail_pattern, book.author, book.genre)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Task.cleanup()
    }

    fun exit() {
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_OPEN -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    openBook(File(data.data.path))
                }
            }
            REQUEST_TEXT -> {
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_book_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_properties ->
                PropertiesActivity.editProperties(this, REQUEST_PROPERTIES, bookList.current.chapter)
            R.id.action_new ->
                newBook()
            R.id.action_open ->
                chooseFile()
            R.id.action_exit ->
                exit()
            else ->
                return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
        menuInflater.inflate(R.menu.menu_book_tree, menu)
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_insert -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                insertChapter(bookList.itemAt(info.position), info.position)
            }
            R.id.action_rename -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                renameChapter(bookList.itemAt(info.position))
            }
            R.id.action_properties -> {
                val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
                PropertiesActivity.editProperties(this, REQUEST_PROPERTIES, bookList.itemAt(info.position).chapter)
            }
            else -> return super.onContextItemSelected(item)
        }
        return true
    }

    override fun onBackPressed() {
        val chapter = bookList.current?.chapter
        if (chapter != null && chapter != Task.book) {
            bookList.backTop()
        } else {
            super.onBackPressed()
        }
    }

    data class ItemHolder(val icon: ImageView, val name: TextView, val detail: TextView, val option: ImageView)

    private inner class ChapterItem(val chapter: Chapter) : AbstractItem() {
        lateinit var holder: ItemHolder

        override fun size(): Int = chapter.size()

        @Suppress("unchecked_cast")
        override fun <T : Item> getParent(): T? = if (chapter.parent != null)
            ChapterItem(chapter.parent) as T
        else null

        override fun isItem(): Boolean = !chapter.isSection

        override fun isGroup(): Boolean = chapter.isSection

        override fun equals(other: Any?): Boolean = when (other) {
            null -> false
            !is ChapterItem -> false
            else -> other.chapter == chapter
        }

        override fun toString(): String = chapter.title
    }

    private inner class BookAdapter(bookList: BookList) : Adapter<ChapterItem>(bookList) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            val holder: ItemHolder

            if (convertView == null) {
                view = View.inflate(this@BookActivity, R.layout.chapter_item, null)
                holder = ItemHolder(
                        view.findViewById(R.id.icon) as ImageView,
                        view.findViewById(R.id.chapter_title) as TextView,
                        view.findViewById(R.id.chapter_overview) as TextView,
                        view.findViewById(R.id.option) as ImageView
                )
                view.tag = holder
            } else {
                view = convertView
                holder = view.tag as ItemHolder
            }

            val item = getItem(position)
            item.holder = holder

            val chapter = item.chapter

            holder.name.text = chapter.title
            holder.detail.text = chapter.intro?.text ?: ""
            if (chapter.isSection) {
                holder.option.visibility = View.VISIBLE
                holder.option.setImageResource(R.drawable.ic_arrow)
            } else {
                holder.option.visibility = View.GONE
            }

            return view
        }
    }

    private inner class BookList(listView: ListView)
        : ListSupport<ChapterItem>(listView, true, true, false, null) {

        override fun makeAdapter(): Adapter<ChapterItem> = BookAdapter(this)

        override fun makeItems(current: ChapterItem, items: MutableList<ChapterItem>) {
            current.chapter.mapTo(items) { ChapterItem(it) }
        }

        override fun onItemChosen(item: ChapterItem) {
            TextActivity.editText(this@BookActivity, REQUEST_TEXT, item.chapter)
        }

        override fun onTopReached() {

        }

        override fun afterFetching(position: Pair<Int, Int>?) {
            super.afterFetching(position)
            refreshTitle()
            val chapter = current.chapter
            setPathBar(null, pathBar, pathBarHolder, pathBarButtonFontSize, pathBarButtonPadding)
            detailBar.text = getQuantityString(R.plurals.book_section_detail, 0, chapter.size())
        }
    }

    private inner class PathButtonListener(val chapter: Chapter) : View.OnClickListener {
        override fun onClick(v: View?) {
            if (chapter != bookList.current.chapter) {
                bookList.gotoItem(ChapterItem(chapter))
            }
        }
    }
}
