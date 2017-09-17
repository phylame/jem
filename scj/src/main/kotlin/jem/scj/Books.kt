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

package jem.scj

import jclp.log.Log
import jclp.setting.MapSettings
import jem.Attributes
import jem.Book
import jem.epm.util.MakerParam
import jem.epm.util.ParserParam
import jem.kotlin.set
import jem.util.JemException
import jem.util.Variants
import mala.core.App
import mala.core.App.tr
import java.io.FileNotFoundException

private const val TAG = "Books"

fun checkInputFormat(format: String, path: String = ""): Boolean = if (format.isEmpty()) {
    App.error(tr("error.input.unknown", path))
    false
} else if (SCI.epmManager.getService(format)?.hasParser() != true) {
    App.error(tr("error.input.unsupported", format))
    false
} else true

fun checkOutputFormat(format: String, path: String = ""): Boolean = if (format.isEmpty()) {
    App.error(tr("error.output.unknown", path))
    false
} else if (SCI.epmManager.getService(format)?.hasMaker() != true) {
    App.error(tr("error.output.unsupported", format))
    false
} else true

fun outParam(book: Book): MakerParam = MakerParam.builder()
        .book(book)
        .output(SCI.output)
        .format(SCI.outputFormat)
        .arguments(MapSettings(SCI.outArguments))
        .build()

fun newBook(attaching: Boolean): Book {
    val book = Book()
    attachBook(book, attaching)
    App.sciAction { onBookOpened(book, null) }
    return book
}

fun openBook(param: ParserParam, attaching: Boolean): Book? {
    try {
        return loadBook(param, attaching)
    } catch (e: FileNotFoundException) {
        App.error(tr("error.misc.noFile", param.file ?: param.input))
    } catch (e: Exception) {
        App.error(tr("error.jem.openFailed", param.file ?: param.input), e)
    }
    return null
}

fun loadBook(param: ParserParam, attaching: Boolean): Book {
    App.sciAction { onOpenBook(param) }
    val book: Book?
    try {
        book = SCI.epmManager.readBook(param)
        if (book == null) {
            throw JemException(tr("error.input.unsupported", param.format))
        }
    } catch (e: Exception) {
        App.sciAction { onOpenFailed(e, param) }
        throw e
    }
    attachBook(book, attaching)
    App.sciAction { onBookOpened(book, param) }
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

fun saveBook(param: MakerParam): String? {
    try {
        return makeBook(param)
    } catch (e: Exception) {
        App.error(tr("error.jem.saveFailed", param.file ?: param.output), e)
        return null
    }
}

fun makeBook(param: MakerParam): String? {
    App.sciAction { onSaveBook(param) }
    try {
        if (!SCI.epmManager.writeBook(param)) {
            throw JemException(tr("error.output.unsupported", param.format))
        }
    } catch (e: Exception) {
        App.sciAction { onSaveFailed(e, param) }
        throw e
    }
    App.sciAction { onBookSaved(param) }
    return param.file?.path ?: param.output
}

private fun attachAttributes(book: Book) {
    for ((k, v) in SCI.outAttributes) {
        try {
            val value = Variants.parse(v.toString(), Attributes.getType(k))
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
