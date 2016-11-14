package pw.phylame.imabw.activity

import android.app.Activity
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDialog
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.Toast
import com.unnamed.b.atv.model.TreeNode
import com.unnamed.b.atv.view.AndroidTreeView
import pw.phylame.imabw.R
import pw.phylame.jem.core.Book
import pw.phylame.jem.core.Chapter
import pw.phylame.jem.epm.EpmManager
import rx.Observable
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import android.content.Intent
import pw.phylame.jem.core.Attributes

class BookActivity : AppCompatActivity() {
    lateinit var book: Book
    private var root = TreeNode.root()
    lateinit var treeView: AndroidTreeView

    private fun makeNode(chapter: Chapter): TreeNode {
        val node = TreeNode(ChapterItem(chapter)).setViewHolder(ChapterHolder(this))
        if (chapter.isSection) {
            for (sub in chapter) {
                node.addChild(makeNode(sub))
            }
        }
        return node
    }

    override fun onStart() {
        System.setProperty(EpmManager.AUTO_LOAD_CUSTOMIZED_KEY, "true")
        super.onStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        treeView = AndroidTreeView(this, root);
        treeView.setDefaultAnimation(true)
        treeView.setDefaultContainerStyle(R.style.TreeNodeStyleDivided, true)
        treeView.setDefaultNodeLongClickListener { node, any ->
            openContextMenu(node.viewHolder.view)
            true
        }
        treeView.setDefaultNodeClickListener { node, any ->
            val value = node.value
            if (value is ChapterItem) {
                val chapter = value.chapter
                if (!chapter.isSection) {
                    val intent = Intent(this, TextActivity::class.java)
                    intent.putExtra("text", chapter.text.text)
                    intent.putExtra("title", Attributes.getTitle(chapter))
                    startActivity(intent)
                }
            }
        }
        val view = findViewById(R.id.content_edit) as ViewGroup
        view.addView(treeView.view)
        registerForContextMenu(view)

        val fab = findViewById(R.id.fab_add) as FloatingActionButton
        fab.setOnClickListener {
            Snackbar.make(it, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show()
        }

        title = "No book opened"
    }

    fun chooseFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File"), 100)
        } catch (ex: android.content.ActivityNotFoundException) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            openBook(File(data.data.path))
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun openBook(file: File, format: String? = null) {
        Observable.create<Book> {
            book = EpmManager.readBook(file, format ?: EpmManager.formatOfFile(file.path), null)
            it.onNext(book)
            it.onCompleted()
        }.flatMap {
            Observable.from(it.items())
        }.map {
            makeNode(it)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : Subscriber<TreeNode>() {
            override fun onError(e: Throwable) {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))

                AlertDialog.Builder(this@BookActivity)
                        .setTitle("Cannot load file")
                        .setMessage(sw.toString())
                        .setPositiveButton("OK") {
                            dialog, which ->
                            dialog.dismiss()
                        }
                        .create().show()
            }

            override fun onNext(node: TreeNode) {
                treeView.addNode(root, node)
            }

            override fun onCompleted() {
                Toast.makeText(this@BookActivity, "Done", Toast.LENGTH_SHORT).show()
                title = Attributes.getTitle(book)
            }
        })
    }

    fun aboutApp() {
        val dialog = AlertDialog.Builder(this)
                .setTitle("Supported formats")
                .setMessage(EpmManager.supportedParsers().joinToString("\n"))
                .setPositiveButton("I Know") {
                    dialog, which ->
                    dialog.dismiss()
                }
                .create()
        dialog.show()
    }

    fun exit() {
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_open -> chooseFile()
            R.id.action_about -> aboutApp()
            R.id.action_exit -> exit()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        menuInflater.inflate(R.menu.menu_tree, menu)
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {
        println(treeView.selected)
        return super.onContextItemSelected(item)
    }
}
