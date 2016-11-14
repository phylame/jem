package pw.phylame.imabw.activity

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.*
import com.unnamed.b.atv.model.TreeNode
import com.unnamed.b.atv.view.AndroidTreeView
import pw.phylame.imabw.R
import pw.phylame.jem.core.Book
import pw.phylame.jem.core.Chapter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class EditActivity : AppCompatActivity() {
    lateinit var book: Book
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val root = TreeNode.root()
        Observable.create<Book> {
            // read the book
            book = Book("Demo")
            for (i in 1..11) {
                val chapter = Chapter("Chapter $i")
                if (i % 3 == 0) {
                    for (j in 1..3) {
                        chapter.append(Chapter("Chapter $i.$j"))
                    }
                }
                book.append(chapter)
            }
            it.onNext(book)
        }.flatMap {
            Observable.from(it.items())
        }.map {
            makeNode(it)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe {
            treeView.addNode(root, it)
        }

        treeView = AndroidTreeView(this, root);
        treeView.setDefaultAnimation(true)
        treeView.setDefaultContainerStyle(R.style.TreeNodeStyleDivided, true)
        treeView.setDefaultNodeLongClickListener { node, any ->
            openContextMenu(node.viewHolder.view)
            true
        }
        val view = findViewById(R.id.content_edit) as ViewGroup
        view.addView(treeView.view)
        registerForContextMenu(view)

        val fab = findViewById(R.id.fab_add) as FloatingActionButton
        fab.setOnClickListener(View.OnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        })
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
