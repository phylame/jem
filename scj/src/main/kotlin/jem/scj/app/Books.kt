/*
 * Copyright 2017 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jem.scj.app

import jclp.log.Log
import jem.Attributes
import jem.Book
import jem.epm.EpmInParam
import jem.epm.EpmManager
import jem.epm.EpmOutParam
import jem.kotlin.set
import jem.util.Variants
import mala.core.App
import mala.core.App.tr
import java.io.File
import java.io.FileNotFoundException

private const val TAG = "Books"

fun checkInputFormat(format: String, path: String = ""): Boolean = if (format.isEmpty()) {
    App.error(tr("error.input.unknown", path))
    false
} else if (!EpmManager.hasParser(format)) {
    App.error(tr("error.input.unsupported", format))
    false
} else true

fun checkOutputFormat(format: String, path: String = ""): Boolean = if (format.isEmpty()) {
    App.error(tr("error.output.unknown", path))
    false
} else if (!EpmManager.hasMaker(format)) {
    App.error(tr("error.output.unsupported", format))
    false
} else true

fun outParam(book: Book) = EpmOutParam(book, File(SCI.output), SCI.outputFormat, SCI.outArguments)

fun newBook(attaching: Boolean): Book {
    val book = Book()
    attachBook(book, attaching)
    App.sciAction { onBookOpened(book) }
    return book
}

fun openBook(param: EpmInParam, attaching: Boolean): Book? {
    try {
        return loadBook(param, attaching)
    } catch (e: FileNotFoundException) {
        App.error(tr("error.misc.noFile", param.path ?: param.file))
    } catch (e: Exception) {
        App.error(tr("error.jem.openFailed", param.path ?: param.file), e)
    }
    return null
}

fun loadBook(param: EpmInParam, attaching: Boolean): Book {
    App.sciAction { onOpenBook(param) }
    val book: Book
    try {
        book = EpmManager.readBook(param)
    } catch (e: Exception) {
        App.sciAction { onOpenFailed(param, e) }
        throw e
    }
    attachBook(book, attaching)
    App.sciAction { onBookOpened(book) }
    return book
}

fun attachBook(book: Book, attaching: Boolean) {
    if (attaching) {
        attachAttributes(book)
    }
    if (attaching) {
        attachExtensions(book)
    }
}

fun saveBook(param: EpmOutParam): String? {
    try {
        return makeBook(param)
    } catch (e: Exception) {
        App.error(tr("error.jem.saveFailed", param.path ?: param.file), e)
        return null
    }
}

fun makeBook(param: EpmOutParam): String? {
    App.sciAction { onSaveBook(param) }
    try {
        EpmManager.writeBook(param)
    } catch (e: Exception) {
        App.sciAction { onSaveFailed(param, e) }
        throw e
    }
    return param.output.path
}

private fun attachAttributes(book: Book) {
    for ((k, v) in SCI.outAttributes) {
        try {
            val value = Variants.parse(Attributes.typeOf(k), v.toString())
            if (value == null) {
                App.error(tr("error.misc.badString", v))
                Log.d(TAG, "cannot convert \"{0}\" to \"{1}\"", v, k)
                continue
            }
            book[k] = value
        } catch (e: RuntimeException) {
            App.error(tr("error.misc.badString", v), e)
        }
    }
}

private fun attachExtensions(book: Book) {
    book.extensions.update(SCI.outExtensions)
}
