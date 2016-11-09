/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Imabw.
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

package pw.phylame.jem.imabw.app

import pw.phylame.jem.core.Book
import pw.phylame.jem.epm.EpmManager
import pw.phylame.jem.imabw.app.ui.Dialogs
import pw.phylame.jem.imabw.app.ui.OpenResult
import pw.phylame.ycl.io.PathUtils
import java.awt.Component
import java.io.File
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
    private val parserExtensions: Array<Any> by lazy {
        val results = HashSet<Any>()
        for (name in EpmManager.supportedParsers()) {
            val extensions = EpmManager.extensionsOfName(name)
            if (extensions.size == 1) {
                results.add(extensions[0])
            } else {
                results.add(extensions)
            }
        }
        results.toArray()
    }

    private val makerExtensions: Array<Any> by lazy {
        val results = HashSet<Any>()
        for (name in EpmManager.supportedMakers()) {
            val extensions = EpmManager.extensionsOfName(name)
            if (extensions.size == 1) {
                results.add(extensions[0])
            } else {
                results.add(extensions)
            }
        }
        results.toArray()
    }


    fun selectBookFile(parent: Component?,
                       title: String,
                       forOpen: Boolean,
                       initFile: File? = null,
                       format: String? = null,
                       multiple: Boolean = false): OpenResult? {
        var _format: String? = null
        if (format.isNullOrEmpty()) {
            if (initFile != null) {
                _format = PathUtils.extensionName(initFile.path)
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
        val extension = PathUtils.extensionName(file.path).toLowerCase()
        val format = EpmManager.nameOfExtension(extension)
        return format ?: extension
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
