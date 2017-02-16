package pw.phylame.imabw.activity

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import jem.kotlin.*
import jem.Attributes
import jem.Book
import jem.Chapter
import jem.epm.EpmInParam
import jem.epm.EpmManager
import jem.util.Build
import pw.phylame.ancotols.*
import pw.phylame.imabw.*
import pw.phylame.imabw.R
import pw.phylame.penguin.PenguinActivity
import rx.Observable
import rx.Observer
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File
import java.io.IOException
import java.util.*

internal class BookActivity : BaseActivity() {
    companion object {
        const val REQUEST_OPEN = 100
        const val REQUEST_TEXT = 101
        const val REQUEST_ATTRIBUTES = 102
        const val REQUEST_EXTENSIONS = 103
    }

    val toolbar: Toolbar by lazyView(R.id.toolbar)
    val pathBar: LinearLayout by lazyView(R.id.path_bar)
    val sectionInfo: TextView by lazyView(R.id.info_bar)
    val progressBar: ProgressBar by lazyView(R.id.progress_bar)

    lateinit var searchView: SearchView
    lateinit var contents: Contents
    val exitChecker = TimedAction(2000L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book)
        setSupportActionBar(toolbar)

        isActionDriven = true

        val list: ListView = this[R.id.list]
        registerForContextMenu(list)
        findViewById(R.id.fab).setOnClickListener { newChapter(contents.item) }

        contents = Contents(list)
        newBook0(null)
    }

//    override fun onStart() {
//        super.onStart()
//        if (Task.restoreState(this)) {
//            contents.current = Item(Task.book!!)
//        } else {
//            newBook(null)
//        }
//        if (Task.book == null) {
//            newBook(null)
//        }
//    }
//
//    override fun onStop() {
//        super.onStop()
//        Task.saveState(this)
//    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_book, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, info: ContextMenu.ContextMenuInfo) {
        menuInflater.inflate(R.menu.menu_toc, menu)
        super.onCreateContextMenu(menu, v, info)
    }

    override fun onBackPressed() {
        if (!contents.backParent()) {
            if (Task.isModified) {
                maybeSaving(R.string.exit_app_title) {
                    super.onBackPressed()
                }
            } else {
                if (exitChecker.isEnable) {
                    super.onBackPressed()
                } else {
                    shortToast(R.string.exit_app_double)
                }
            }
        }
    }

    @MenuAction(R.id.action_attributes)
    fun editAttributes() {
        editAttributes(contents.item)
    }

    @MenuAction(R.id.action_new)
    fun newBook() {
        newBook(null)
    }

    @MenuAction(R.id.action_open)
    fun openBook() {
        maybeSaving(R.string.open_book_title) {
            PenguinActivity.perform(this, REQUEST_OPEN)
        }
    }

    @MenuAction(R.id.action_save)
    fun saveBook() {
        saveBook(IgnoredAction)
    }

    @MenuAction(R.id.action_convert)
    fun convertBook() {

    }

    @MenuAction(R.id.action_help)
    fun help() {

    }

    @MenuAction(R.id.action_about)
    fun about() {
        val b = StringBuilder()
        b.append("Jem: ${Build.VERSION} by ${Build.VENDOR}").append("\n")
        b.append("Supported Parser:\n")
        b.append("  ").append(EpmManager.supportedParsers().joinToString(postfix = "\n", transform = String::toUpperCase))
        b.append("Supported Maker:\n")
        b.append("  ").append(EpmManager.supportedMakers().joinToString(postfix = "\n", transform = String ::toUpperCase))
        message("About Imabw", b)
    }

    @MenuAction(R.id.action_exit)
    fun exit() {
        maybeSaving(R.string.exit_app_title) {
            Task.cleanup()
            finishAll()
            System.exit(0)
        }
    }

    @MenuAction(R.id.toc_action_add)
    fun addChapter(item: MenuItem) {
        newChapter(contents[item.position], titleId = R.string.add_subchapter_title)
    }

    @MenuAction(R.id.toc_action_insert)
    fun insertChapter(item: MenuItem) {
        newChapter(contents.item, item.position, R.string.insert_chapter_title)
    }

    @MenuAction(R.id.toc_action_rename)
    fun renameChapter(item: MenuItem) {
        val chapter = contents[item.position].chapter
        input(R.string.rename_chapter_title, chapter.title, R.string.add_chapter_hint) {
            chapter.title = it
            fireModified(true)
        }
    }

    @MenuAction(R.id.toc_action_attributes)
    fun chapterAttributes(item: MenuItem) {
        editAttributes(contents[item.position])
    }

    @ResultAction(REQUEST_OPEN)
    fun onFileChosen(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val path = data.getStringExtra(PenguinActivity.RESULT_KEY).split(",").first()
            openBook0(EpmInParam(File(path), null, null, null))
        }
    }

    @ResultAction(REQUEST_ATTRIBUTES)
    fun onAttributesEdited(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val item = Task.chapter!!
            if (item.chapter === Task.book) {
                updateTitle()
            } else {
                contents._adapter.refreshView(item)
            }
            Task.chapter = null
        }
    }

    fun activateTask(book: Book, param: EpmInParam? = null) {
        Task.reset(book, param)
        Item.root = book
        contents.activateItem(Item(book))
    }

    fun maybeSaving(titleId: Int, action: () -> Unit) {
        if (!Task.isModified) {
            action()
        } else {
            confirm(titleId, R.string.save_book_asking, cancelId = R.string.button_discard) { ok ->
                if (ok) {
                    saveBook(action)
                } else {
                    action()
                }
            }
        }
    }

    fun newBook(title: String?) {
        maybeSaving(R.string.new_book_title) {
            newBook0(title)
        }
    }

    fun newBook0(title: String?) {
        showProgress(this, progressBar, true)
        openProgress(true)
        Observable.create<Book> {
            it.onNext(createBook(title ?: getString(R.string.comm_book_untitled)))
        }.subscribeOn(Schedulers.computation())
                .doOnNext {
                    Task.cleanup()
                    System.gc()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    openProgress(false)
                    activateTask(it)
                }
    }

    fun openBook0(param: EpmInParam) {
        if (!param.file.exists()) {
            message(R.string.open_book_title, R.string.err_no_such_file, param.file)
            return
        }
        if (!EpmManager.hasParser(param.format)) {
            message(R.string.open_book_title, R.string.err_unsupported_format, param.file)
            return
        }
        openProgress(true)
        Observable.create<Pair<Book, EpmInParam>> {
            it.onNext(readBook(param, param.format == EpmManager.PMAB) to param)
            it.onCompleted()
        }.subscribeOn(Schedulers.io())
                .doOnNext {
                    Task.cleanup()
                    System.gc()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Pair<Book, EpmInParam>> {
                    override fun onNext(r: Pair<Book, EpmInParam>) {
                        activateTask(r.first, r.second)
                    }

                    override fun onCompleted() {
                        openProgress(false)
                    }

                    override fun onError(e: Throwable) {
                        openProgress(false)
                        noteOpenError(e, param)
                    }
                })
    }

    fun saveBook(success: () -> Unit) {
        // todo: choose output if need
        openProgress(true)
        Observable.create<Any> {
            Thread.sleep(2000)
            it.onCompleted()
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Any> {
                    override fun onNext(t: Any?) {
                    }

                    override fun onCompleted() {
                        openProgress(false)
                        fireModified(false)
                        shortToast(R.string.save_book_success, "/sdcard/ex.pmab")
                        success()
                    }

                    override fun onError(e: Throwable) {
                        openProgress(false)
                    }
                })
    }

    fun noteOpenError(e: Throwable, param: EpmInParam) {
        Log.e(TAG, "open book error:", e)
        val message = when (e) {
            is IOException -> getString(R.string.err_bad_file, param.file)
            else -> getString(R.string.err_unknown_cause)
        }
        message(getString(R.string.open_book_failed), message)
    }

    fun openProgress(shown: Boolean) {
        showProgress(this, progressBar, shown)
    }

    fun newChapter(item: Item, index: Int = -1, titleId: Int = R.string.add_chapter_title) {
        input(titleId, getString(R.string.comm_chapter_untitled), R.string.add_chapter_hint, true) {
            val chapter = Chapter(it)
            if (index < 0) {
                contents.append(item, chapter)
            } else {
                contents.insert(item, index, chapter)
            }
            fireModified(true)
        }
    }

    fun removeChapter(item: Item, position: Int) {
        fireModified(true)
    }

    fun editAttributes(item: Item) {
        AttributesActivity.perform(this, REQUEST_ATTRIBUTES, item)
    }

    fun fireModified(modified: Boolean) {
        Task.isModified = modified
        updateUI()
    }

    fun updateUI() {
        updateTitle()
        sectionInfo.text = getQuantityString(R.plurals.book_section_detail, 0, contents.size)
    }

    fun updateTitle() {
        val book = Task.book
        if (book != null) {
            if (toolbar.logo != null) {
                (toolbar.logo as BitmapDrawable).bitmap.recycle()
            }
            val cover = book.cover?.openStream()
            if (cover != null) {
                val opt = BitmapFactory.Options()
                val bmp = BitmapFactory.decodeStream(cover, null, opt)
                val m = ThumbnailUtils.extractThumbnail(bmp, (toolbar.height * 0.75).toInt(), toolbar.height)
                bmp.recycle()
                toolbar.logo = BitmapDrawable(resources, m)
            } else {
                toolbar.logo = null
            }
            val b = StringBuilder()
            if (Task.isModified) {
                b.append('*')
            }
            b.append(book.title.or { getString(R.string.comm_book_untitled) })
            toolbar.title = b
            if (book.author.isNotEmpty()) {
                b.setLength(0)
                b.append(book.author)
                if (book.genre.isNotEmpty()) {
                    b.append(" | ").append(book.genre)
                }
                toolbar.subtitle = b
            } else {
                toolbar.subtitle = null
            }
        } else {
            toolbar.setTitle(R.string.app_name)
            toolbar.subtitle = null
        }
    }

    inner class ChapterInfoPopup {
        val title: TextView
        val popup: PopupWindow
        lateinit var item: Item

        init {
            val view = LayoutInflater.from(this@BookActivity).inflate(R.layout.popup_chapter_info, null)
            popup = PopupWindow(view, WRAP_CONTENT, WRAP_CONTENT, true)
            popup.isTouchable = true

            title = view[R.id.title]
            view.findViewById(R.id.title_bar).setOnClickListener {
                popup.dismiss()
                editAttributes(item)
            }
        }

        fun showInfo(item: Item) {
            this.item = item
            title.text = item.chapter.title
            popup.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
        }
    }

    val chapterInfoPopup by lazy { ChapterInfoPopup() }

    fun attrPopup(item: Item) {
        chapterInfoPopup.showInfo(item)
    }

    inner class Contents(list: ListView) : ListNGModel<Item>() {
        val _adapter: Adapter
        private lateinit var items: MutableList<Item>

        init {
            _adapter = Adapter(this@BookActivity)
            setup(list, _adapter)
        }

        val size: Int get() = items.size

        fun append(parent: Item, chapter: Chapter) {
            parent.chapter.append(chapter)
            parent.count++
            if (parent.chapter === contents.item.chapter) { // appended to current item
                items.add(Item(chapter))
                adapter.notifyDataSetChanged()
                if (contents.size - list.firstVisiblePosition < 16) {
                    list.smoothScrollToPosition(contents.size - 1)
                }
                if (contents.size > fastScrollLimit) {
                    list.isFastScrollEnabled = true
                }
            } else {
                _adapter.refreshView(parent)
            }
            fireModified(true)
        }

        fun insert(parent: Item, index: Int, chapter: Chapter) {
            parent.chapter.insert(index, chapter)
            parent.count++
            if (parent.chapter === contents.item.chapter) { // inserted to current item
                items.add(index, Item(chapter))
                adapter.notifyDataSetChanged()
                if (contents.size > fastScrollLimit) {
                    list.isFastScrollEnabled = true
                }
            } else {
                _adapter.refreshView(parent)
            }
            fireModified(true)
        }

        operator fun get(index: Int): Item = items[index]

        override fun onFetchItems(parent: Item): List<Item> {
            items = ArrayList<Item>(parent.count)
            parent.chapter.mapTo(items, ::Item)
            return items
        }

        private val itemColor: Int by lazyColor(R.color.pathBarTextColor)

        private val itemSize: Float by lazyDimen(R.dimen.book_path_font_size)

        override fun onPostFetch() {
            updateUI()
            refreshPathBar(pathBar, itemSize, itemColor)
        }

        override fun onItemClicked(item: Item, view: View, position: Int, id: Long) {
            TextActivity.perform(this@BookActivity, REQUEST_TEXT, item)
        }
    }
}

internal class Item(val chapter: Chapter) : ListNGItem() {
    companion object {
        lateinit var root: Chapter
    }

    var count: Int = chapter.size()

    override val parent: ListNGItem? get() {
        if (isRoot) {
            return null
        } else {
            return Item(chapter.parent ?: return null)
        }
    }

    override val isGroup: Boolean get() = chapter.isSection

    override val isRoot: Boolean get() = chapter === root

    override val name: CharSequence get() = chapter.title

    override fun toString(): String = "Item(chapter=$chapter)"

    override fun hashCode(): Int = chapter.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        return when (other) {
            !is Item -> false
            else -> chapter === other.chapter
        }
    }
}

internal class Holder(view: View) {
    val icon: ImageView = view[R.id.icon]
    val title: TextView = view[R.id.title]
    val intro: TextView = view[R.id.intro]
    val detail: TextView = view[R.id.detail]
    val option: ImageView = view[R.id.option]

    fun setIntro(text: CharSequence?) {
        if (text == null) {
            intro.visibility = View.GONE
        } else {
            intro.text = text
            intro.visibility = View.VISIBLE
        }
    }

    fun setDetail(text: CharSequence?) {
        if (text == null) {
            detail.visibility = View.GONE
        } else {
            detail.text = text
            detail.visibility = View.VISIBLE
        }
    }
}

internal class Adapter(val activity: BookActivity) : ListNGAdapter<Item>() {

    val defaultCover: Drawable by lazy {
        ContextCompat.getDrawable(activity, R.drawable.cover)
    }

    val iconWidth by lazy {
        activity.resources.getDimensionPixelSize(R.dimen.chapter_icon_width)
    }

    val iconHeight by lazy {
        activity.resources.getDimensionPixelSize(R.dimen.chapter_icon_height)
    }

    fun refreshView(item: Item) {
        val holder = item.view?.tag as Holder?
        if (holder != null) {
            refreshView(item, holder)
        }
    }

    fun refreshView(item: Item, holder: Holder) {
        val chapter = item.chapter
        holder.title.text = chapter.title
        holder.icon.tag = item
        holder.icon.setOnClickListener {
            activity.attrPopup(it.tag as Item)
        }
        val oldIcon = holder.icon.drawable
        val stream = chapter.cover?.openStream()
        if (stream != null) {
            val bmp = BitmapFactory.decodeStream(stream)
            val m = ThumbnailUtils.extractThumbnail(bmp, iconWidth, iconHeight)
            bmp.recycle()
            holder.icon.setImageBitmap(m)
        } else {
            holder.icon.setImageDrawable(defaultCover)
        }
        val intro = chapter.intro?.text
        if (intro.isNullOrEmpty()) {
            holder.setIntro(null)
        } else {
            holder.setIntro(activity.getString(R.string.chapter_intro_info, intro))
        }
        if (oldIcon != null && oldIcon !== defaultCover) {
            (oldIcon as BitmapDrawable).bitmap.recycle()
        }
        val size = chapter.size()
        if (size > 0) {
            holder.setDetail(activity.resources.getQuantityString(R.plurals.book_section_detail, size, size))
            holder.option.visibility = View.VISIBLE
            holder.option.setImageResource(R.drawable.ic_arrow)
        } else {
            val words = chapter.attributes[Attributes.WORDS]
            var text: CharSequence? = null
            if (words != null) {
                text = activity.getString(R.string.chapter_words_info, words)
            }
            holder.setDetail(text)
            holder.option.visibility = View.GONE
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val (view, holder) = reusedView(convertView, activity, R.layout.chapter_item, ::Holder)
        val item = getItem(position)
        refreshView(item, holder)
        item.view = view
        view.setOnTouchListener(object : View.OnTouchListener {
            var dx: Float = 0F
            var ux: Float = 0F
            var isShown = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dx = e.x
                        if (isShown) {
                            println("hidden button")
                            isShown = false
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        ux = e.x
                        println("ux=$ux, dx=$dx")
                        if (Math.abs(dx - ux) > 20) {
                            println("show button")
                            isShown = true
                            return true
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        return true
                    }
                }
                return false
            }

        })
        return view
    }
}
