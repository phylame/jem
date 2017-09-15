package jem.epm

import jclp.io.extName
import jclp.setting.Settings
import jem.Book
import jem.title
import java.io.File

data class ParserParam(val path: String, val format: String = "", val arguments: Settings? = null) {
    val epmName get() = format.takeIf(String::isNotEmpty) ?: extName(path)
}

data class MakerParam(val book: Book, val path: String, val format: String = "", val arguments: Settings? = null) {
    val output: String get() = File(path).let { if (!it.isDirectory) it else File(it, "${book.title}.$format") }.path

    val epmName get() = format.takeIf(String::isNotEmpty) ?: extName(output)
}