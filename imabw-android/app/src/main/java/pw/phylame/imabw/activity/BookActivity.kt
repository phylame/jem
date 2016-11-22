package pw.phylame.imabw.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.widget.*
import pw.phylame.imabw.R
import pw.phylame.jem.core.Book
import pw.phylame.jem.core.Chapter
import pw.phylame.jem.epm.EpmManager
import pw.phylame.seal.BaseActivity
import pw.phylame.seal.SealActivity
import pw.phylame.seal.UIs
import rx.Observable
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File
import java.util.*

class BookActivity : BaseActivity() {
    companion object {
        private val TAG = BookActivity::class.java.simpleName
    }

    private lateinit var book: Book

    private lateinit var adapter: BookAdapter

    private lateinit var toolbar: Toolbar
    private lateinit var tvDetail: TextView
    private lateinit var listView: ListView
    private var progress: ProgressBar? = null

    override fun onStart() {
        System.setProperty(EpmManager.AUTO_LOAD_CUSTOMIZED_KEY, "true")
        super.onStart()
    }

    var splash: ImageView? = null

    fun showSplash() {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val splash = ImageView(this)
        splash.scaleType = ImageView.ScaleType.FIT_XY
        splash.setImageResource(R.drawable.ic_splash)
        setContentView(splash)
        this.splash = splash
    }

    fun initUI() {
        setContentView(R.layout.activity_book)
        toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        tvDetail = findViewById(R.id.chapter_detail) as TextView
        listView = findViewById(R.id.list) as ListView
        registerForContextMenu(listView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (splash == null) {
            showSplash()
            Handler().postDelayed({
                splash = null
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                initUI()
            }, 1000)
        }
    }

    fun chooseFile() {
        SealActivity.startMe(this, 100, true, false, false, false, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            openBook(File(data.data.path))
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun showProgress(shown: Boolean) {
        if (progress == null) {
            progress = findViewById(R.id.progress) as ProgressBar
        }
        progress!!.visibility = if (shown) View.VISIBLE else View.GONE
    }

    fun openBook(file: File, format: String? = null, arguments: Map<String, Any>? = null) {
        showProgress(true)
        toolbar.setTitle(R.string.book_open_progress)
        toolbar.subtitle = null

        Observable.create<Book> {
            book = EpmManager.readBook(file, format ?: EpmManager.formatOfFile(file.path), arguments)
            it.onNext(book)
            it.onCompleted()
        }.flatMap {
            Observable.from(it.items())
        }.map(::Item).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Subscriber<Item>() {
                    override fun onError(e: Throwable) {
                        Log.e(TAG, "failed to load book", e)
                        showProgress(false)
                        refreshTitle()
                        UIs.alert(this@BookActivity, getString(R.string.book_open_book_failed), e.message)
                    }

                    override fun onNext(node: Item) {

                    }

                    override fun onCompleted() {
                        showProgress(false)
                        refreshTitle()
                    }
                })
    }

    private fun refreshTitle() {
        toolbar.title = book.title
        toolbar.subtitle = getString(R.string.book_detail_pattern, book.author, book.genre)
    }

    override fun onDestroy() {
        super.onDestroy()
        book.cleanup()
    }

    fun exit() {
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_book_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
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
}

class Item(chapter: Chapter)

class BookAdapter(val context: Context) : BaseAdapter() {
    val items: List<Item> = ArrayList()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        if (convertView == null) {
            view = View.inflate(context, R.layout.chapter_item, null)
        } else {
            view = convertView
        }
        return view
    }

    override fun getItem(position: Int): Item = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = items.size

}
