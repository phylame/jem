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

package jem.epm

import jclp.Reusable
import jclp.ReusableHelper
import jclp.log.Log
import jclp.releaseSelf
import jclp.setting.Settings
import jclp.setting.getInt
import jclp.setting.getString
import jclp.vdm.VDMManager
import jclp.vdm.VDMReader
import jclp.vdm.VDMWriter
import jclp.vdm.detectReader
import jem.Book
import jem.M
import java.io.Closeable
import java.io.File
import java.io.IOException

interface CommonParser<I : Closeable> : Parser {
    fun open(input: String, arguments: Settings?): I

    fun parse(input: I, arguments: Settings?): Book

    override fun parse(input: String, arguments: Settings?) = open(input, arguments).let { source ->
        try {
            parse(source, arguments)
        } catch (e: Exception) {
            throw e
        } finally {
            source.releaseSelf()
        }
    }
}

interface CommonMaker<O : Closeable> : Maker {
    fun open(output: String, arguments: Settings?): O

    fun make(book: Book, output: O, arguments: Settings?)

    override fun make(book: Book, output: String, arguments: Settings?) {
        open(output, arguments).use { make(book, it, arguments) }
    }
}

interface VDMParser : CommonParser<VDMReader> {
    override fun open(input: String, arguments: Settings?): VDMReader = arguments?.getString("parser.vdm.type")?.let {
        VDMManager.openReader(it, input)?.let(::VDMReaderWrapper)
    } ?: detectReader(File(input)).let(::VDMReaderWrapper)
}

interface VDMMaker : CommonMaker<VDMWriter> {
    override fun open(output: String, arguments: Settings?) = (arguments?.getString("maker.vdm.type") ?: "zip").let {
        VDMManager.openWriter(it, output) ?: throw IOException(M.tr("err.vdm.unsupported", it))
    }.apply {
        arguments?.getString("maker.vdm.comment")?.takeIf(String::isNotEmpty)?.let { setComment(it) }
        arguments?.getInt("maker.vdm.zipLevel")?.let { setProperty("level", it) }
        arguments?.getInt("maker.vdm.zipMethod")?.let { setProperty("method", it) }
    }
}

private class VDMReaderWrapper(val reader: VDMReader) : VDMReader by reader, Reusable {
    override fun retain() = helper.retain()

    override fun release() = helper.release()

    val helper = object : ReusableHelper() {
        override fun dispose() {
            Log.d("VDMReaderWrapper") { "'$reader' is closing for no reference" }
            reader.close()
        }
    }

    override fun toString() = reader.toString()
}
