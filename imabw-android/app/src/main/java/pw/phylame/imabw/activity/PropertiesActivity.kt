package pw.phylame.imabw.activity

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import pw.phylame.android.util.BaseActivity
import pw.phylame.imabw.R
import pw.phylame.jem.core.Attributes
import pw.phylame.jem.core.Chapter
import pw.phylame.jem.util.Variants
import java.util.*

class PropertiesActivity : BaseActivity() {
    companion object {
        fun editProperties(invoker: Activity, requestCode: Int, chapter: Chapter) {
            Task.chapter = chapter
            invoker.startActivityForResult(PropertiesActivity::class.java, requestCode)
        }

        private val ignoreAttributes = setOf(
                Attributes.TITLE
        )
    }

    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_properties)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        listView = findViewById(R.id.list) as ListView

        findViewById(R.id.fab).setOnClickListener { addProperty() }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        initList()
    }

    private fun initList() {
        val attributes = Task.chapter!!.attributes
        val list = ArrayList<Item>()
        attributes.entries().filter {
            it.key !in ignoreAttributes
        }.sortedWith(Comparator<Map.Entry<String, Any>> {
            o1, o2 ->
            o1.key.compareTo(o2.key)
        }).forEach {
            list.add(Item(it.key, it.value))
        }
        listView.adapter = PropertiesAdapter(list)
    }

    private fun addProperty() {

    }

    private class Item(val name: String, var value: Any)

    private class Holder(val name: TextView, val value: TextView)

    private inner class PropertiesAdapter(val list: MutableList<Item>) : BaseAdapter() {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            val holder: Holder
            if (convertView == null) {
                view = View.inflate(this@PropertiesActivity, android.R.layout.simple_list_item_2, null)
                holder = Holder(
                        view.findViewById(android.R.id.text1) as TextView,
                        view.findViewById(android.R.id.text2) as TextView
                )
                view.tag = holder
            } else {
                view = convertView
                holder = view.tag as Holder
            }

            val item = getItem(position)
            val name = item.name
            holder.name.text = getString(R.string.chapter_attribute_name,
                    Attributes.titleOf(name) ?: name.capitalize(),
                    name)
            holder.value.text = Variants.printable(item.value) ?: item.value.toString()

            return view
        }

        override fun getItem(position: Int): Item = list[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getCount(): Int = list.size

    }
}
