package pw.phylame.imabw.activity

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.text.method.KeyListener
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import pw.phylame.android.util.BaseActivity
import pw.phylame.imabw.R
import pw.phylame.jem.core.Chapter
import pw.phylame.jem.title

class TextActivity : BaseActivity() {

    companion object {
        fun editText(invoker: Activity, requestCode: Int, chapter: Chapter) {
            Task.chapter = chapter
            invoker.startActivityForResult(TextActivity::class.java, requestCode)
        }
    }

    private lateinit var text: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val chapter = Task.chapter!!

        text = findViewById(R.id.text) as EditText
        val str = chapter.text?.text ?: ""
        if (str.isNotEmpty()) {
            text.text = str
        }
        toggleEditable()

        title = chapter.title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun toggleEditable(): Boolean {
        if (text.keyListener == null) {
            text.keyListener = text.tag as KeyListener
            return true
        } else {
            text.tag = text.keyListener
            text.keyListener = null
            return false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chapter_text, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_readonly -> {
                item.setIcon(if (toggleEditable()) R.drawable.ic_unlocked else R.drawable.ic_locked)
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

}
