package pw.phylame.imabw.activity

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.widget.EditText
import pw.phylame.imabw.R
import pw.phylame.seal.BaseActivity

class TextActivity : BaseActivity() {

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
