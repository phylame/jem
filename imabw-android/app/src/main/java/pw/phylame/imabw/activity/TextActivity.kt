package pw.phylame.imabw.activity

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.text.method.KeyListener
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import jem.core.Chapter
import jem.title
import pw.phylame.ancotols.MenuAction
import pw.phylame.ancotols.startActivityForResult
import pw.phylame.ancotols.lazyView
import pw.phylame.imabw.BaseActivity
import pw.phylame.imabw.R

internal class TextActivity : BaseActivity() {

    companion object {
        fun perform(invoker: Activity, requestCode: Int, chapter: Item) {
            Task.chapter = chapter
            invoker.startActivityForResult(TextActivity::class.java, requestCode)
        }
    }

    val text: EditText by lazyView(R.id.text)
    val toolbar: Toolbar by lazyView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text)
        setSupportActionBar(toolbar)

        val chapter = Task.chapter!!.chapter

        val str = chapter.text?.text ?: ""
        if (str.isNotEmpty()) {
            text.setText(str)
        }
        toggleEditable()

        title = chapter.title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun toggleEditable(): Boolean {
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

    @MenuAction(R.id.action_readonly)
    fun toggleReadonly(item: MenuItem) {
        item.setIcon(if (toggleEditable()) R.drawable.ic_unlocked else R.drawable.ic_locked)
    }
}
