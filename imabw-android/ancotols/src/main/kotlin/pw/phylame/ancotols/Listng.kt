package pw.phylame.ancotols

import android.os.AsyncTask
import android.support.annotation.ColorInt
import android.support.annotation.Dimension
import android.support.v7.widget.AppCompatTextView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.*

abstract class ListNGItem {
    // view for this item, null for invisible item
    open var view: View? = null

    // selected state of the item
    open var isSelected: Boolean = false

    // readable name of the item
    open val name: CharSequence get() = toString()

    // get the parent item of current item
    abstract val parent: ListNGItem?

    abstract val isGroup: Boolean

    abstract val isRoot: Boolean
}

abstract class ListNGAdapter<T> : BaseAdapter() {
    var items: List<T> = emptyList()
        internal set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): T = items[position]

    override fun getItemId(position: Int): Long = position.toLong()
}

abstract class ListNGModel<T : ListNGItem> : AdapterView.OnItemClickListener {
    companion object {
        private val INITIAL_POSITION = 0 to -1
    }

    // multiple selection mode
    var isMultiple = false

    // selection mode for group
    var isGroupMode = false

    // fetch items in async mode
    var isAsyncMode = false

    // limit of item count for fast scroll
    var fastScrollLimit = 50

    // current shown item
    lateinit var item: T
        private set

    lateinit var list: ListView
        private set

    lateinit var adapter: ListNGAdapter<T>
        private set

    private val selections = LinkedHashSet<T>()

    private val positions = LinkedList<Pair<Int, Int>>()

    fun setup(list: ListView, adapter: ListNGAdapter<T>) {
        this.list = list
        this.adapter = adapter
        list.adapter = adapter
        list.onItemClickListener = this
    }

    open fun onPreFetch() {
    }

    abstract fun onFetchItems(parent: T): List<T>

    open fun onPostFetch() {
    }

    open fun onItemToggled(item: T) {
    }

    open fun onItemClicked(item: T, view: View, position: Int, id: Long) {
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val item = adapter.getItem(position)
        if (item.isGroup) {
            enterChild(item)
        } else if (isMultiple) {
            toggleSelection(item)
        } else {
            onItemClicked(item, view, position, id)
        }
    }

    // refresh sub-items of current item
    fun refresh(keepPosition: Boolean = true) {
        selections.clear()
        refreshItems(if (keepPosition) topPositionOf(list) else INITIAL_POSITION)
    }

    // go to the specified child item
    fun enterChild(item: T) {
        positions.addLast(topPositionOf(list))
        activateItem0(item, INITIAL_POSITION)
    }

    // back up to the parent item of current item
    fun backParent(): Boolean {
        if (item.isRoot) {
            return false
        }
        val parent = item.parent ?: throw IllegalStateException("non-root item must have valid parent")
        @Suppress("unchecked_cast")
        activateItem0(parent as T, positions.removeLast())
        return true
    }

    fun getSelections(): Set<T> = selections

    fun toggleSelection(item: T, selected: Boolean? = null) {
        item.isSelected = selected ?: !item.isSelected
        if (item.isSelected) {
            selections.add(item)
        } else {
            selections.remove(item)
        }
        onItemToggled(item)
    }

    fun toggleSelections(selected: Boolean? = null) {
        if (!isMultiple) {
            return
        }
        for (item in adapter.items) {
            if (isGroupMode) {
                if (item.isGroup) {
                    toggleSelection(item, selected)
                }
            } else if (!item.isGroup) {
                toggleSelection(item, selected)
            }
        }
    }

    // set the specified item as current item
    fun activateItem(item: T) {
        positions.clear()
        activateItem0(item, INITIAL_POSITION)
    }

    protected fun topPositionOf(list: ListView): Pair<Int, Int> {
        var index = list.firstVisiblePosition
        var view = list.getChildAt(0)
        var top = if (view == null) 0 else view.top
        if (top < 0 && list.getChildAt(1) != null) {
            index++
            view = list.getChildAt(1)
            top = view!!.top
        }
        return index to top
    }

    @Suppress("unchecked_cast")
    protected fun currentToRoot(): LinkedList<Pair<T, Pair<Int, Int>>> {
        val it = positions.descendingIterator()
        val results = LinkedList<Pair<T, Pair<Int, Int>>>()
        results.add(this.item to INITIAL_POSITION)
        var item: T? = this.item.parent as T?
        while (item != null) {
            results.add(item to if (it.hasNext()) it.next() else INITIAL_POSITION)
            item = item.parent as T?
        }
        return results
    }

    protected fun refreshPathBar(bar: ViewGroup, textSize: Float, @ColorInt textColor: Int) {
        bar.removeAllViews()
        val items = currentToRoot()
        val end = items.size - 1
        items.descendingIterator().asSequence().forEachIndexed { i, pair ->
            val view = createPathItem(pair.first.name, textSize, textColor)
            view.setOnClickListener { view ->
                if (pair.first != item) {
                    var position: Pair<Int, Int> = INITIAL_POSITION
                    for (j in view.tag as Int..positions.size - 1) {
                        position = positions.removeLast()
                    }
                    activateItem0(pair.first, position)
                }
            }
            view.tag = i
            bar.addView(view)
            if (i != end) {
                bar.addView(createPathItem(" > ", textSize, textColor))
            }
        }
        val holder = bar.parent
        if (holder is HorizontalScrollView) {
            holder.post {
                holder.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
            }
        } else if (holder is ScrollView) {
            holder.post {
                holder.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun createPathItem(text: CharSequence, size: Float, color: Int): View {
        val view = AppCompatTextView(list.context)
        view.gravity = Gravity.CENTER
        view.setTextColor(color)
        view.textSize = size
        view.text = text
        return view
    }

    private fun activateItem0(item: T, position: Pair<Int, Int>) {
        this.item = item
        refreshItems(position)
    }

    private fun refreshItems(position: Pair<Int, Int>) {
        if (isAsyncMode) {
            AsyncLoader().execute(position)
        } else {
            onPreFetch()
            onItemsFetched(onFetchItems(item), position)
            onPostFetch()
        }
    }

    private fun onItemsFetched(items: List<T>, position: Pair<Int, Int>) {
        adapter.items = items
        list.isFastScrollEnabled = adapter.count > fastScrollLimit
        list.setSelectionFromTop(position.first, if (position.second < 0) 0 else position.second)
    }

    inner class AsyncLoader : AsyncTask<Pair<Int, Int>, Unit, List<T>>() {
        lateinit var position: Pair<Int, Int>

        override fun doInBackground(vararg params: Pair<Int, Int>): List<T> {
            position = params.first()
            return onFetchItems(item)
        }

        override fun onPreExecute() {
            onPreFetch()
        }

        override fun onPostExecute(items: List<T>) {
            onItemsFetched(items, position)
            onPostFetch()
        }
    }
}
