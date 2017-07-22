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

package jem.imabw.app

import jclp.io.FileUtils
import jclp.io.PathUtils
import jclp.log.Log
import jem.Book
import jem.Chapter
import jem.epm.EpmManager
import jem.imabw.app.ui.Dialogs
import jem.imabw.app.ui.OpenResult
import jem.imabw.app.ui.Viewer
import jem.imabw.app.ui.WaitingDialog
import qaf.core.App
import rx.Observable
import rx.Observer
import rx.functions.Action0
import rx.schedulers.Schedulers
import rx.schedulers.SwingScheduler
import java.awt.Component
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

// keeps parameters for Jem epm parser
class EpmInParam(var file: File, extension: String?, var arguments: Map<String, Any>, var cache: File? = null) {
    // name of the epm parser
    var format: String? = if (extension.isNullOrEmpty()) {
        Books.detectFormat(file)
    } else {
        EpmManager.nameOfExtension(extension) ?: extension
    }
}

// keeps parameters for Jem epm maker
class EpmOutParam(var book: Book, var file: File, extension: String, var arguments: Map<String, Any>) {
    // name of epm maker
    var format: String = EpmManager.nameOfExtension(extension) ?: extension
}

open class OpeningObserver : Observer<Any>, Action0 {
    internal var dialog: WaitingDialog? = null

    override fun onNext(r: Any) {
        when (r) {
            is EpmInParam -> {
                dialog?.updateWaiting(r.file.path)
            }
            is Pair<*, *> -> {
                onBook(r.first as Book, r.second as EpmInParam)
            }
        }
    }

    override fun onCompleted() {
        dialog?.isVisible = false
    }

    override fun onError(e: Throwable) {
        dialog?.isVisible = false
    }

    override fun call() {
        onCancel()
    }

    open fun onBook(book: Book, param: EpmInParam) {

    }

    open fun onCancel() {

    }
}

// utilities for book
object Books {
    val TAG = "Books"

    private fun deleteFile(file: File?) {
        if (file?.delete() ?: false) {
            Log.e(TAG, "failed to delete file: {0}", file)
        }
    }

    fun dumpError(err: Throwable): String {
        val str: String
        if (err is UnsupportedFormatException) {
            str = App.tr("d.error.unsupportedFormat", err.format)
        } else if (err is FileNotFoundException) {
            str = App.tr("d.error.fileNotExists")
        } else {
            str = err.message ?: ""
        }
        return str
    }

    fun showOpenError(parent: Component?, title: String, param: EpmInParam, err: Throwable) {
        val str = App.tr("d.openBook.failed", param.file, dumpError(err))
        Dialogs.trace(parent, title, str, err)
    }

    fun showSaveError(parent: Component?, title: String, param: EpmOutParam, e: Throwable) {
        val str = App.tr("d.saveBook.failed", param.file, dumpError(e))
        Dialogs.trace(parent, title, str, e)
    }

    fun selectBook(parent: Component?,
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
        if (args == null || args.isEmpty()) {
            args = Books.getParseArguments(parent, format) ?: return null
        }
        return EpmInParam(_file!!, _format, args)
    }

    fun readBook(param: EpmInParam, cached: Boolean = false): Book {
        var input: File = param.file
        var cache: File? = null
        if (cached) {
            try {
                cache = File.createTempFile("_imabw_book_", ".tmp")
                FileUtils.copyFile(param.file, cache)
                input = cache
            } catch (e: IOException) {
                Log.e(TAG, "cannot create dump file", e)
                if (cache != null) {
                    deleteFile(cache)
                    cache = null
                }
            }
        }

        val book: Book
        try {
            book = EpmManager.readBook(input, param.format, param.arguments)
        } catch (e: Exception) {
            deleteFile(cache)
            throw e
        }

        if (cache != null) {
            param.cache = cache
            book.registerCleanup { deleteFile(cache) }
        }

        return book
    }

    fun openBook(title: String,
                 cached: Boolean,
                 params: Array<EpmInParam>,
                 observer: OpeningObserver) {
        val dialog = Dialogs.waiting(Viewer, title, App.tr("d.openBook.tip"), "")
        dialog.cancellable = true
        dialog.progressable = true
        observer.dialog = dialog
        val subscription = Observable.create<Any> {
            for (param in params) {
                it.onNext(param)
                it.onNext(readBook(param, cached) to param)
            }
            it.onCompleted()
        }.subscribeOn(Schedulers.io())
                .observeOn(SwingScheduler.getInstance())
                .doOnUnsubscribe(observer)
                .subscribe(observer)
        dialog.cancelAction = {
            subscription.unsubscribe()
        }
        dialog.showForResult(false)
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
            name = Dialogs.inputText(parent, title, App.tr("d.newBook.inputTip"), App.tr("d.newBook.defaultTitle"), false, false)
            if (name == null) {
                return null
            }
        }
        val book = Book(name)
        book.append(Chapter(App.tr("d.newChapter.defaultTitle")))
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

    val defaultInArguments: Map<String, Any> by lazy {
        emptyMap<String, Any>()
    }

    val defaultOutArguments: Map<String, Any> by lazy {
        emptyMap<String, Any>()
    }

    fun getParseArguments(parent: Component?, format: String?): Map<String, Any>? {
        return HashMap()
    }

    fun getMakeArguments(parent: Component?, format: String?): Map<String, Any>? {
        return HashMap()
    }
}
