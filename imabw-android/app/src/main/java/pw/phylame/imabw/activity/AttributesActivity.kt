package pw.phylame.imabw.activity

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import jem.core.Attributes
import jem.core.Attributes.titleOf
import jem.core.Attributes.typeOf
import jem.core.Chapter
import jem.title
import jem.util.Variants
import jem.util.Variants.printable
import jem.util.text.Text
import jem.util.text.Texts
import pw.phylame.ancotols.*
import pw.phylame.imabw.BaseActivity
import pw.phylame.imabw.R
import pw.phylame.imabw.datetime
import pw.phylame.imabw.input
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*

internal class AttributesActivity : BaseActivity(), AdapterView.OnItemClickListener {
    companion object {
        fun perform(invoker: Activity, requestCode: Int, item: Item) {
            Task.chapter = item
            invoker.startActivityForResult(AttributesActivity::class.java, requestCode)
        }

        val ignoreAttributes = setOf<String>()

        val bookNames = Attributes.supportedNames()
                .filter { it !in ignoreAttributes }
                .toSortedSet()
    }

    val toolbar: Toolbar by lazyView(R.id.toolbar)

    val list: ListView by lazyView(R.id.list)

    var isModified = false

    lateinit var chapter: Chapter

    lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attributes)
        setSupportActionBar(toolbar)

        findViewById(R.id.fab).setOnClickListener { addAttribute() }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        list.onItemClickListener = this
        adapter = Adapter()
        list.adapter = adapter

        if (Task.chapter == null) {
            shortToast(R.string.err_internal_cause)
            finish()
        } else {
            chapter = Task.chapter!!.chapter
            toolbar.subtitle = chapter.title
            adapter.refresh()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isModified) {
            Task.isModified = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_attributes, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val item = adapter.items[position]
        when (Variants.typeOf(item.value) ?: "") {
            Variants.STRING -> editString(item, view.tag as Holder)
            Variants.BOOLEAN -> editBoolean(item, view.tag as Holder)
            Variants.DATETIME -> editDatetime(item, view.tag as Holder)
            Variants.INTEGER -> Unit
            Variants.LOCALE -> Unit
            Variants.REAL -> Unit
            Variants.FLOB -> editFlob(item, view.tag as Holder)
            Variants.TEXT -> editText(item, view.tag as Holder)
            else -> editString(item, view.tag as Holder)
        }
    }

    fun editString(item: Map.Entry<String, Any>, holder: Holder) {
        val key = item.key
        val name = titleOf(key) ?: key.capitalize()
        input(getString(R.string.edit_attribute_tip, name), item.value as CharSequence) {
            chapter.attributes[key] = it
            holder.value.text = it
            setResult(Activity.RESULT_OK)
            isModified = true
        }
    }

    fun editBoolean(item: Map.Entry<String, Any>, holder: Holder) {
        val value = !(item.value as Boolean)
        chapter.attributes[item.key] = value
        holder.value.text = toString(value)
        setResult(Activity.RESULT_OK)
        isModified = true
    }

    fun editText(item: Map.Entry<String, Any>, holder: Holder) {
        val key = item.key
        val name = titleOf(key) ?: key.capitalize()
        input(getString(R.string.edit_attribute_tip, name), (item.value as Text).text) {
            chapter.attributes[key] = Texts.forString(it, Texts.PLAIN)
            holder.value.text = it
            setResult(Activity.RESULT_OK)
            isModified = true
        }
    }

    fun editFlob(item: Map.Entry<String, Any>, holder: Holder) {
        val key = item.key
        if (key == Attributes.COVER) {
            startActivityForResult(ImageActivity::class.java, 100) {

            }
        }
    }

    fun editDatetime(item: Map.Entry<String, Any>, holder: Holder) {
        datetime()
    }

    fun toString(value: Any): String = when (value) {
        is Boolean -> getString(if (value) R.string.comm_true else R.string.comm_false)
        else -> printable(value) ?: value.toString()
    }

    fun addAttribute() {
        val keys = bookNames.filter { it !in chapter.attributes }.toMutableList()
        AlertDialog.Builder(this)
                .setTitle(R.string.add_attribute_title)
                .setItems(keys.map(::titleOf).toTypedArray()) { dialog, which ->
                    chapter.attributes[keys[which]] = Variants.defaultOf(typeOf(keys[which]))
                    isModified = true
                    setResult(Activity.RESULT_OK)
                    adapter.refresh()
                }
                .create()
                .show()
    }

    @MenuAction(R.id.action_clear)
    fun clearAttributes() {
        chapter.attributes.clear()
        setResult(Activity.RESULT_OK)
        isModified = true
        adapter.refresh()
    }

    inner class Holder(view: View) {
        val name: TextView = view[android.R.id.text1]
        val value: TextView = view[android.R.id.text2]
    }

    inner class Adapter : BaseAdapter() {
        val items = ArrayList<Map.Entry<String, Any>>()

        fun refresh() {
            Observable.create<Unit> {
                items.setAll(chapter.attributes.entries().filter {
                    it.key !in ignoreAttributes
                }.sortedWith(Comparator<Map.Entry<String, Any>> {
                    o1, o2 ->
                    o1.key.compareTo(o2.key)
                }))
                it.onNext(Unit)
            }.subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        adapter.notifyDataSetChanged()
                    }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val (view, holder) = reusedView(convertView, this@AttributesActivity, android.R.layout.simple_list_item_2) {
                Holder(it)
            }
            val item = items[position]
            val name = item.key
            holder.name.text = titleOf(name) ?: name.capitalize()
            holder.value.text = toString(item.value)
            return view
        }

        override fun getItem(position: Int): Map.Entry<String, Any> = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getCount(): Int = items.size
    }
}
