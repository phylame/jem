package pw.phylame.imabw.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.AppCompatTextView
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
import pw.phylame.jem.core.Book
import pw.phylame.jem.core.Chapter
import pw.phylame.jem.epm.EpmManager
import pw.phylame.jem.util.text.Texts
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
        private const val REQUEST_TEXT = 200
    }

    private var book: Book? = null
    private lateinit var bookList: BookList

    private lateinit var toolbar: Toolbar
    private lateinit var pathBarHolder: View
    private lateinit var pathBar: LinearLayout
    private lateinit var detailBar: TextView

    private val pathBarFontSize: Float by lazy {
        UIs.dimenFor(resources, R.dimen.book_path_font_size)
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

        pathBarHolder = findViewById(R.id.sv_path_bar)
        pathBar = findViewById(R.id.path_bar) as LinearLayout
        detailBar = findViewById(R.id.section_detail) as TextView

        val listView = findViewById(R.id.list) as ListView
        registerForContextMenu(listView)

        bookList = BookList(listView, null)
        newBook()
    }

    fun chooseFile() {
        SealActivity.choose(this, REQUEST_OPEN, true, false, false, false, null)
    }

    private fun showProgress(shown: Boolean) {
        if (progressBar == null) {
            progressBar = findViewById(R.id.progress_bar) as ProgressBar
        }
        progressBar!!.visibility = if (shown) View.VISIBLE else View.GONE
    }

    fun newBook(title: String? = null) {
        book?.cleanup()
        book = Book(title ?: getString(R.string.book_default_book_name))
        val book = this.book!!
        book.author = "PW"
        book.genre = "Magic, WP"
        val intro = Texts.forString("This is intro of book", Texts.PLAIN)
        book.intro = intro
        for (i in 1..10) {
            var chapter = Chapter("Sub Chapter $i")
            chapter.intro = intro
            book.append(chapter)
            if (i % 3 == 0) {
                for (j in 1..5) {
                    val ch = Chapter("${chapter.title}.$j")
                    chapter.append(ch)
                    ch.intro = intro
                    if (j % 2 == 0) {
                        for (k in 1..5) {
                            val c = Chapter("${ch.title}.$k")
                            c.intro = intro
                            ch.append(c)
                        }
                    }
                }
            }
        }
        bookList.refresh(ChapterItem(book))
    }

    fun openBook(file: File, format: String? = null, arguments: Map<String, Any>? = null) {
        showProgress(true)
        toolbar.setTitle(R.string.book_open_progress)
        toolbar.subtitle = null

        Observable.create<Book> {
            book = EpmManager.readBook(file, format ?: EpmManager.formatOfFile(file.path), arguments)
            it.onNext(book)
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
                        bookList.refresh(ChapterItem(book))
                    }

                    override fun onCompleted() {
                        showProgress(false)
                    }
                })
    }

    private fun refreshTitle() {
        val book = this.book
        if (book != null) {
            toolbar.title = book.title
            toolbar.subtitle = getString(R.string.book_detail_pattern, book.author, book.genre)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        book?.cleanup()
    }

    fun exit() {
        finish()
    }

    fun setChapterPath(chapter: Chapter) {
        val chapters = LinkedList<Chapter>()
        var ch: Chapter? = chapter
        while (ch != null) {
            chapters.addFirst(ch)
            ch = ch.parent
        }
        if (pathBarHolder.visibility != View.VISIBLE) {
            pathBarHolder.visibility = View.VISIBLE
        }
        pathBar.removeAllViews()
        val end = chapters.size - 1
        chapters.forEachIndexed { i, chapter ->
            pathBar.addView(createPathButton(chapter))
            if (i != end) {
                pathBar.addView(createPathSeparator())
            }
        }
    }

    private fun createPathButton(chapter: Chapter): View {
        val button = AppCompatTextView(this)
        button.textSize = pathBarFontSize
        button.text = chapter.title
        val padding = resources.getDimensionPixelSize(R.dimen.book_path_margin)
        button.setPadding(0, padding, 0, padding)
        button.setOnClickListener(PathButtonListener(chapter))
        return button
    }

    private fun createPathSeparator(): View {
        val text = AppCompatTextView(this)
        text.textSize = pathBarFontSize
        text.text = " > "
        return text
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_book_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new -> newBook()
            R.id.action_open -> chooseFile()
            R.id.action_exit -> exit()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        menuInflater.inflate(R.menu.menu_book_tree, menu)
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {
        return super.onContextItemSelected(item)
    }

    override fun onBackPressed() {
        val chapter = bookList.current?.chapter
        if (chapter != null && chapter != book) {
            bookList.backTop()
        } else {
            super.onBackPressed()
            exit()
        }
    }

    data class ItemHolder(val icon: ImageView, val name: TextView, val detail: TextView, val option: ImageView)

    private inner class ChapterItem(val chapter: Chapter) : AbstractItem() {
        lateinit var holder: ItemHolder

        private var _parent: ChapterItem? = null

        override fun size(): Int = chapter.size()

        override fun setParent(parent: Item?) {
            _parent = parent as? ChapterItem
        }

        @Suppress("unchecked_cast")
        override fun <T : Item> getParent(): T? = _parent as? T

        override fun isItem(): Boolean = !chapter.isSection

        override fun isGroup(): Boolean = chapter.isSection
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

    private inner class BookList(listView: ListView, chapterItem: ChapterItem?)
        : ListSupport<ChapterItem>(listView, true, true, false, chapterItem) {

        override fun makeAdapter(): Adapter<ChapterItem> = BookAdapter(this)

        override fun makeItems(current: ChapterItem, items: MutableList<ChapterItem>) {
            current.chapter.mapTo(items) { ChapterItem(it) }
        }

        override fun onChoosing(item: ChapterItem) {
            TextActivity.editText(this@BookActivity, REQUEST_TEXT, item.chapter)
        }

        override fun afterFetching(position: Pair<Int, Int>?) {
            super.afterFetching(position)
            refreshTitle()
            val chapter = current.chapter
            setChapterPath(chapter)
            detailBar.text = this@BookActivity.getQuantityString(R.plurals.book_section_detail, 0, chapter.size())
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
