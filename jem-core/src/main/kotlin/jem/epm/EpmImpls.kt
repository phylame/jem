/*
 * Copyright 2015-2017 Peng Wan <phylame@163.com>
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

import jclp.managed
import jclp.setting.Settings
import jclp.setting.getInt
import jclp.setting.getString
import jclp.vdm.*
import jem.Book
import jem.M
import java.io.Closeable
import java.nio.file.Paths

const val PMAB_NAME = "pmab"
const val PARSER_VDM_TYPE_KEY = "parser.vdm.type"

const val MAKER_VDM_TYPE_KEY = "maker.vdm.type"
const val MAKER_VDM_COMMENT_KEY = "maker.vdm.comment"
const val MAKER_VDM_ZIP_LEVEL_KEY = "maker.vdm.zipLevel"
const val MAKER_VDM_ZIP_METHOD_KEY = "maker.vdm.zipMethod"

interface FileParser

interface CommonParser<I : Closeable> : Parser {
    fun open(input: String, arguments: Settings?): I

    fun parse(input: I, arguments: Settings?): Book

    override fun parse(input: String, arguments: Settings?): Book = managed(open(input, arguments)) {
        parse(it, arguments)
    }
}

interface CommonMaker<O : Closeable> : Maker {
    fun open(output: String, arguments: Settings?): O

    fun make(book: Book, output: O, arguments: Settings?)

    override fun make(book: Book, output: String, arguments: Settings?) {
        open(output, arguments).use { make(book, it, arguments) }
    }
}

interface VdmParser : CommonParser<VdmReader>, FileParser {
    override fun open(input: String, arguments: Settings?): VdmReader =
            arguments?.getString(PARSER_VDM_TYPE_KEY)?.let {
                VdmManager.openReader(it, input) ?: throw ParserException(M.tr("err.vdm.unsupported", it))
            } ?: detectReader(Paths.get(input))
}

interface VdmMaker : CommonMaker<VdmWriter> {
    override fun open(output: String, arguments: Settings?): VdmWriter {
        val props = hashMapOf<String, Any>()
        arguments?.getInt(MAKER_VDM_ZIP_LEVEL_KEY)?.let { props["level"] = it }
        arguments?.getInt(MAKER_VDM_ZIP_METHOD_KEY)?.let { props["method"] = it }
        return (arguments?.getString(MAKER_VDM_TYPE_KEY) ?: VDM_ZIP).let {
            VdmManager.openWriter(it, output, props) ?: throw MakerException(M.tr("err.vdm.unsupported", it))
        }.apply {
            arguments?.getString(MAKER_VDM_COMMENT_KEY)?.takeIf { it.isNotEmpty() }?.let { setComment(it) }
        }
    }
}
