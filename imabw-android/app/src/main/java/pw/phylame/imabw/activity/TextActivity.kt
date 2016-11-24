package pw.phylame.imabw.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.widget.EditText
import android.widget.TextView
import pw.phylame.android.util.BaseActivity
import pw.phylame.imabw.R
import pw.phylame.jem.core.Chapter

class TextActivity : BaseActivity() {

    companion object {
        fun editText(invoker: Activity, requestCode: Int, chapter: Chapter) {
            val intent = Intent(invoker, TextActivity::class.java)
            intent.putExtra("title", chapter.title)
            intent.putExtra("text", chapter.text?.text ?: "")
            invoker.startActivityForResult(intent, requestCode)
        }
    }

    private lateinit var text: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val text = findViewById(R.id.text) as EditText
        val str = intent.getStringExtra("text")
        if (str.isNotEmpty()) {
            text.setText(str)
        }

        title = intent.getStringExtra("title") ?: "Untitled"
    }

}
