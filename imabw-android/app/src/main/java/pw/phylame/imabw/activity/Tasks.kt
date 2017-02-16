package pw.phylame.imabw.activity

import android.app.Activity
import android.support.annotation.WorkerThread
import jem.Book
import jem.epm.EpmInParam

internal object Task {
//    val taskPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    // current edited book
    var book: Book? = null

    var inParam: EpmInParam? = null

    var isModified = false

    // current edited chapter
    var chapter: Item? = null

    fun reset(book: Book, inParam: EpmInParam? = null) {
        this.book = book
        this.inParam = inParam
        this.isModified = false
    }

    @WorkerThread
    fun cleanup() {
        if (book != null) {
            book!!.cleanup()
            book = null
        }
        inParam = null
        chapter = null
        isModified = false
    }

    fun restoreState(activity: Activity): Boolean {
//        val preferences = activity.getPreferences(Activity.MODE_PRIVATE)
//        preferences.all.forEach(::println)
//        val path = preferences.getString("undone_book", null)
        return false
    }

    fun saveState(activity: Activity) {
        if (isModified) {
//            Thread {
//                val file = File.createTempFile("_imabw_", ".tmp")
//                EpmManager.writeBook(book!!, file, EpmManager.PMAB, null)
//                val editor = activity.getPreferences(Activity.MODE_PRIVATE).edit()
//                editor.putString("undone_book", file.path)
//                editor.putString("source_format", inParam?.format)
//                editor.putString("cache_path", inParam?.cache?.absolutePath)
//                editor.putString("source_path", inParam?.file?.absolutePath)
//                editor.apply()
//            }.start()
        }
    }
}
