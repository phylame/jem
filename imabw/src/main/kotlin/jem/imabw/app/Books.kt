/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

package jem.imabw.app

import jem.core.Book
import jem.core.Chapter
import jem.epm.EpmManager
import jem.imabw.app.ui.Dialogs
import jem.imabw.app.ui.OpenResult
import jem.util.UnsupportedFormatException
import pw.phylame.commons.io.IOUtils
import pw.phylame.commons.io.PathUtils
import pw.phylame.commons.log.Log
import pw.phylame.qaf.core.App
import pw.phylame.qaf.core.tr
import rx.Observable
import rx.Observer
import rx.schedulers.Schedulers
import rx.schedulers.SwingScheduler
import java.awt.Component
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

class EpmInParam(var file: File, extension: String?, var arguments: Map<String, Any>, var cached: Boolean) {
    var format: String? = if (extension.isNullOrEmpty()) {
        Books.detectFormat(file)
    } else {
        EpmManager.nameOfExtension(extension) ?: extension
    }

    companion object {
        fun getOrSelect(parent: Component?,
                        title: String,
                        file: File? = null,
                        format: String? = null,
                        arguments: Map<String, Any>? = null): EpmInParam? {
            var _file: File? = file
            val _format: String?
            if (file == null) {
                val result = Books.selectOpenBook(parent, title, null, format, false) ?: return null
                _file = result.file
                _format = result.format
            } else {
                _format = null
            }
            var args: Map<String, Any>? = arguments
            if (args == null) {
                args = Books.getParseArguments(parent, format) ?: return null
            }
            return EpmInParam(_file!!, _format, args, false)
        }
    }
}

class EpmOutParam(var book: Book, var file: File, extension: String, var arguments: Map<String, Any>) {
    var format: String = EpmManager.nameOfExtension(extension) ?: extension
}

object Books {
    val TAG: String = javaClass.name

    private fun deleteFile(file: File?) {
        if (file?.delete() ?: false) {
            Log.e(TAG, "failed to delete file: {0}", file)
        }
    }

    fun dumpError(err: Throwable): String {
        val str: String
        if (err is UnsupportedFormatException) {
            str = tr("d.error.unsupportedFormat", err.format)
        } else if (err is FileNotFoundException) {
            str = tr("d.error.fileNotExists")
        } else {
            str = err.message!!
        }
        return str
    }

    fun showOpenError(parent: Component?, title: String, param: EpmInParam, err: Throwable) {
        val str = tr("d.openBook.failed", param.file, dumpError(err))
        Dialogs.trace(parent, title, str, err)
    }

    fun showSaveError(parent: Component?, title: String, param: EpmOutParam, e: Throwable) {
        val str = tr("d.saveBook.failed", param.file, dumpError(e))
        Dialogs.trace(parent, title, str, e)
    }

    fun readBook(param: EpmInParam): Pair<Book, File?> {
        var input: File
        var cache: File? = null
        if (param.cached) {
            try {
                cache = File.createTempFile("imabw_", ".tmp")
                IOUtils.copyFile(param.file, cache)
                input = cache
            } catch (e: IOException) {
                App.error("cannot create cache file", e)
                Log.e(TAG, e)
                cache = null
                input = param.file
            }
        } else {
            input = param.file
        }

        val book: Book
        try {
            book = EpmManager.readBook(input, param.format, param.arguments)
        } catch (e: Exception) {
            deleteFile(cache)
            throw e
        }

        if (cache != null) {
            book.registerCleanup { deleteFile(cache) }
        }

        return book to cache
    }

    fun openBook(params: Array<out EpmInParam>, task: (() -> Unit)? = null, observer: Observer<Triple<Book, File?, EpmInParam>>) {
        Observable.create<Triple<Book, File?, EpmInParam>> {
            for (param in params) {
                val result = readBook(param)
                it.onNext(Triple(result.first, result.second, param))
            }
            task?.invoke()
            it.onCompleted()
        }.subscribeOn(Schedulers.io())
                .observeOn(SwingScheduler.getInstance())
                .subscribe(observer)
    }

    private val parserExtensions: Array<Any> by lazy {
        val extensions = HashSet<Any>()
        EpmManager.supportedParsers()
                .map { EpmManager.extensionsOfName(it) }
                .forEach {
                    extensions.add(if (it.size == 1) it[0] else it)
                }
        extensions.toArray()
    }

    private val makerExtensions: Array<Any> by lazy {
        val extensions = HashSet<Any>()
        EpmManager.supportedMakers()
                .map { EpmManager.extensionsOfName(it) }
                .forEach {
                    extensions.add(if (it.size == 1) it[0] else it)
                }
        extensions.toArray()
    }

    fun newBook(parent: Component?, title: String, name: String?): Book? {
        var name = name
        if (name.isNullOrEmpty()) {
            name = Dialogs.inputText(parent, title, tr("d.newBook.inputTip"), tr("d.newBook.defaultTitle"), false, false)
            if (name == null) {
                return null
            }
        }
        val book = Book(name)
        book.append(Chapter(tr("d.newChapter.defaultTitle")))
        return book
    }

    fun selectBookFile(parent: Component?,
                       title: String,
                       forOpen: Boolean,
                       initFile: File? = null,
                       format: String? = null,
                       multiple: Boolean = false): OpenResult? {
        var _format: String? = format
        if (_format.isNullOrEmpty()) {
            if (initFile != null) {
                _format = PathUtils.extName(initFile.path)
                if (_format.isEmpty()) {
                    _format = EpmManager.PMAB
                }
            } else {
                _format = EpmManager.PMAB
            }
        }
        return if (forOpen) {
            Dialogs.selectOpenFile(parent, title, initFile, parserExtensions, _format, true, multiple)
        } else {
            Dialogs.selectSaveFile(parent, title, initFile, makerExtensions, _format, false)
        }
    }

    fun selectOpenBook(parent: Component?,
                       title: String,
                       file: File? = null,
                       format: String? = null,
                       multiple: Boolean = false): OpenResult? {
        return selectBookFile(parent, title, true, file, format, multiple)
    }

    fun detectFormat(file: File): String {
        val extension = PathUtils.extName(file.path).toLowerCase()
        return EpmManager.nameOfExtension(extension) ?: extension
    }

    val defaultInParam: Map<String, Any> by lazy {
        emptyMap<String, Any>()
    }

    val defaultOutParam: Map<String, Any> by lazy {
        emptyMap<String, Any>()
    }

    fun getParseArguments(parent: Component?, format: String?): Map<String, Any>? {
        return HashMap()
    }

    fun getMakeArguments(parent: Component?, format: String?): Map<String, Any>? {
        return HashMap()
    }
}
