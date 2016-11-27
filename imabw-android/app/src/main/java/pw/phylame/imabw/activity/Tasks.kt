package pw.phylame.imabw.activity

import pw.phylame.jem.core.Book
import pw.phylame.jem.core.Chapter

object Task {
    // path of current book file
    var path: String? = null

    // current edited book
    var book: Book? = null

    // current edited chapter
    var chapter: Chapter? = null

    fun cleanup() {
        book?.cleanup()
    }
}
