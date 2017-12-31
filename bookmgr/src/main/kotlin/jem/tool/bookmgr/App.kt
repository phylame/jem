package jem.tool.bookmgr

import jclp.io.Flob
import jclp.text.or
import jem.*
import jem.crawler.EXT_CRAWLER_SOURCE_SITE
import jem.epm.EpmManager
import jem.epm.ParserParam
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.statement.Update
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant

fun main(args: Array<String>) {
    val jdbi = Jdbi.create("jdbc:sqlite:E:/tmp/1/book.db")
    jdbi.useHandle<Exception> {
        initDb(it)
    }
    Files.walk(Paths.get("E:/tmp/1"))
            .parallel()
            .filter { it.fileName.toString().endsWith(".pmab") }
            .forEach { path ->
                EpmManager.readBook(ParserParam(path.toString()))?.let {
                    try {
                        addBook(it, path, jdbi)
                    } finally {
                        it.cleanup()
                    }
                }
            }
}

private fun addBook(book: Book, path: Path, jdbi: Jdbi) {
    val assetId = addAsset(path, jdbi)
    book.cover?.let { addCover(it, jdbi) }
    val category = book.extensions[EXT_CRAWLER_SOURCE_SITE].toString() or "default"
    val tagIds = book[KEYWORDS]?.toString()?.split(Attributes.VALUE_SEPARATOR)?.let { addTags(it, jdbi) }
    val authorIds = addAuthors(book.author.split(Attributes.VALUE_SEPARATOR), category, jdbi)
    val genreId = addGenre(book.genre, category, jdbi)
    jdbi.useTransaction<Exception> {
        var bookId = it.selectInteger("select id from book where asset_id=?", assetId)
        if (bookId == null) {
            bookId = it.createUpdate("insert into book(title,genre_id,asset_id,intro) values(?,?,?,?)")
                    .bind(0, book.title)
                    .bind(1, genreId)
                    .bind(2, assetId)
                    .bind(3, book.intro.toString())
                    .executeAndReturnGeneratedKeys()
                    .mapTo(Int::class.java)
                    .findOnly()
        }
        for (authorId in authorIds) {
            val id = it.selectInteger("select id from book_author where author_id=? and author_id=?", authorId, bookId!!)
            if (id == null) {
                it.execute("insert into book_author(author_id,book_id,role) values(?,?,'author')", authorId, bookId)
            }
        }
        tagIds?.forEach { tagId ->
            val id = it.selectInteger("select id from book_tag where tag_id=? and book_id=?", tagId, bookId!!)
            if (id == null) {
                it.execute("insert into book_tag(tag_id,book_id) values(?,?)", tagId, bookId)
            }
        }
    }
}

private fun addTags(tags: Iterable<String>, jdbi: Jdbi): List<Int> {
    return jdbi.inTransaction<List<Int>, Exception> {
        val ids = ArrayList<Int>()
        for (tag in tags) {
            val id = it.selectInteger("select id from tag where name=?", tag)
            ids += id ?: it.createUpdate("insert into tag(name) values(?)")
                    .bind(0, tag)
                    .executeAndReturnGeneratedKeys()
                    .mapTo(Int::class.java)
                    .findOnly()
        }
        ids
    }
}

private fun addAuthors(authors: Iterable<String>, category: String, jdbi: Jdbi): List<Int> {
    return jdbi.inTransaction<List<Int>, Exception> {
        val ids = ArrayList<Int>()
        for (author in authors) {
            val id = it.selectInteger("select id from author where name=? and category=?", author, category)
            ids += id ?: it.createUpdate("insert into author(name,category) values(?,?)")
                    .bind(0, author)
                    .bind(1, category)
                    .executeAndReturnGeneratedKeys()
                    .mapTo(Int::class.java)
                    .findOnly()
        }
        ids
    }
}

private fun addCover(cover: Flob, jdbi: Jdbi) {

}

private fun addGenre(genre: String, category: String, jdbi: Jdbi): Int {
    if (genre.isEmpty()) return -1
    val genres = genre.split('/')
    return jdbi.inTransaction<Int, Exception> {
        var parentId: Int? = null
        for (stub in genres) {
            val id = it.selectInteger("select id from genre where name=? and category=?", stub, category)
            if (id != null) {
                parentId = id
                continue
            }
            parentId = it.createUpdate("insert into genre(name,category,parent_id) values(?,?,?)")
                    .bind(0, stub)
                    .bind(1, category)
                    .bind(2, parentId)
                    .executeAndReturnGeneratedKeys("id")
                    .mapTo(Int::class.java)
                    .findOnly()
        }
        parentId
    }
}

private fun addAsset(path: Path, jdbi: Jdbi): Int {
    val (md5, sha1) = fileCheck(path)
    return jdbi.inTransaction<Int, Exception> {
        val id = it.selectInteger("select id from asset where md5=? or sha1=?", md5, sha1)
        id ?: it.createUpdate("insert into asset(name,mime,sha1,md5,url,created) values(?,?,?,?,?,?)")
                .bindValues(path.fileName.toString(),
                        Files.probeContentType(path),
                        sha1,
                        md5,
                        path.toUri(),
                        Instant.now().epochSecond)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .findOnly()
    }
}

fun Update.bindValues(vararg args: Any) = apply {
    args.forEachIndexed { index, arg ->
        bind(index, arg)
    }
}

private fun fileCheck(path: Path): Pair<String, String> {
    val md5 = MessageDigest.getInstance("md5")
    val sha1 = MessageDigest.getInstance("sha1")
    val buffer = ByteBuffer.allocate(4096)
    Files.newByteChannel(path, StandardOpenOption.READ).use {
        while (it.read(buffer) != -1) {
            buffer.flip()
            md5.update(buffer)
            buffer.flip()
            sha1.update(buffer)
            buffer.clear()
        }
    }
    return md5.digest().toHexString() to sha1.digest().toHexString()
}

val HEX_DIGITS = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

private fun ByteArray.toHexString(): String {
    val b = StringBuilder(size * 2)
    for (i in 0 until size) {
        val n = this[i].toInt()
        b.append(HEX_DIGITS[n and 0xF0 shr 4])
        b.append(HEX_DIGITS[n and 0x0F])
    }
    return b.toString()
}

fun Handle.selectBoolean(sql: String, vararg args: Any): Boolean? = select(sql, *args)
        .mapTo(Boolean::class.java)
        .findFirst()
        .orElse(null)

fun Handle.selectInteger(sql: String, vararg args: Any): Int? = select(sql, *args)
        .mapTo(Int::class.java)
        .findFirst()
        .orElse(null)

private fun initDb(handle: Handle) {
    handle.useTransaction<Exception> {
        //        resetTables(it)
        createTables(it)
    }
}

private fun resetTables(handle: Handle) {
    handle.execute("drop table if exists asset")
    handle.execute("drop table if exists tag")
    handle.execute("drop table if exists genre")
    handle.execute("drop table if exists author")
    handle.execute("drop table if exists book")
    handle.execute("drop table if exists book_author")
    handle.execute("drop table if exists book_tag")
    createTables(handle)
}

private fun createTables(handle: Handle) {
    handle.execute("""
        create table if not exists asset(
          id integer primary key autoincrement not null,
          name text not null,
          mime text not null,
          sha1 text not null,
          md5 text not null,
          url text not null,
          created integer not null,
          modified integer,
          description text
        )
    """)
    handle.execute("create index if not exists idx_asset_name on asset(name)")
    handle.execute("create unique index if not exists idx_asset_sha1 on asset(sha1)")
    handle.execute("create unique index if not exists idx_asset_md5 on asset(md5)")

    handle.execute("""
        create table if not exists tag(
          id integer primary key autoincrement not null,
          name text not null,
          description text
        )
        """)
    handle.execute("create unique index if not exists idx_tag_name on tag(name)")

    handle.execute("""
        create table if not exists genre(
          id integer primary key autoincrement not null,
          name text not null,
          category text not null,
          parent_id integer,
          description text
        )
        """)
    handle.execute("create unique index if not exists idx_genre_name_category on genre(name,category)")

    handle.execute("""
        create table if not exists author(
          id integer primary key autoincrement not null,
          name text not null,
          category text not null,
          description text
        )
        """)
    handle.execute("create unique index if not exists idx_author_name_category on author(name,category)")

    handle.execute("""
        create table if not exists book(
          id integer primary key autoincrement not null,
          title text not null,
          cover_id integer,
          genre_id integer not null,
          asset_id integer not null,
          intro text
        )
        """)
    handle.execute("create index if not exists idx_book_title on book(title)")

    handle.execute("""
        create table if not exists book_author(
          id integer primary key autoincrement not null,
          book_id integer not null,
          author_id integer not null,
          role text not null
        )
        """)
    handle.execute("""
        create table if not exists book_tag(
          id integer primary key autoincrement not null,
          book_id integer not null,
          tag_id integer not null
        )
        """)
}
