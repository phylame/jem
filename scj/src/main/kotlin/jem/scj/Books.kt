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

import jclp.Variants
import jclp.text.Converters
import jem.Attributes
import jem.Book
import jem.JemException
import jem.epm.EpmManager
import jem.epm.MakerParam
import jem.epm.ParserParam
import mala.App
import mala.App.tr
import java.io.FileNotFoundException

fun checkInputFormat(format: String, path: String = "") = when {
    format.isEmpty() -> {
        App.error(tr("err.input.unknown", path))
        false
    }
    EpmManager[format]?.hasParser != true -> {
        App.error(tr("err.input.unsupported", format))
        false
    }
    else -> true
}

fun checkOutputFormat(format: String, path: String = "") = when {
    format.isEmpty() -> {
        App.error(tr("err.output.unknown", path))
        false
    }
    EpmManager[format]?.hasMaker != true -> {
        App.error(tr("err.output.unsupported", format))
        false
    }
    else -> true
}

fun outParam(book: Book) = MakerParam(book, SCI.output, SCI.outputFormat, SCI.outArguments)

fun newBook(attaching: Boolean) = Book().also {
    attachBook(it, attaching)
    App.sciAction { onBookOpened(it, null) }
}

fun openBook(param: ParserParam, attaching: Boolean): Book? {
    try {
        return loadBook(param, attaching)
    } catch (e: FileNotFoundException) {
        App.error(tr("err.misc.noFile", param.path))
    } catch (e: Exception) {
        App.error(tr("err.jem.openFailed", param.path), e)
    }
    return null
}

fun loadBook(param: ParserParam, attaching: Boolean): Book {
    App.sciAction { onOpenBook(param) }
    val book: Book
    try {
        book = EpmManager.readBook(param) ?: throw JemException(tr("err.input.unsupported", param.format))
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
        attachExtensions(book)
    }
}

fun saveBook(param: MakerParam) = try {
    if (SCI["force"] == true) {
        param.arguments?.set("maker.file.overwrite", true)
    }
    makeBook(param)
} catch (e: FileAlreadyExistsException) {
    App.error(tr("err.output.existedFile", e.file, "--force"))
    null
} catch (e: Exception) {
    App.error(tr("err.jem.saveFailed", param.path), e)
    null
}

fun makeBook(param: MakerParam): String {
    App.sciAction { onSaveBook(param) }
    val output: String
    try {
        output = EpmManager.writeBook(param) ?: throw JemException(tr("err.output.unsupported", param.format))
    } catch (e: Exception) {
        App.sciAction { onSaveFailed(e, param) }
        throw e
    }
    App.sciAction { onBookSaved(param) }
    return output
}

private fun attachAttributes(book: Book) {
    for ((k, v) in SCI.outAttributes) {
        try {
            val clazz = Variants.getClass(Attributes.getType(k) ?: continue) ?: continue
            Converters.parse(v.toString(), clazz)?.let { book[k] = it } ?: App.error(tr("err.misc.badString", v))
        } catch (e: Exception) {
            App.error(tr("err.misc.badString", v), e)
        }
    }
}

private fun attachExtensions(book: Book) {
    book.extensions.plusAssign(SCI.outExtensions)
}
