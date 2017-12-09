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

import jclp.KeyedService
import jclp.ServiceManager
import jclp.io.exists
import jclp.io.isDirectory
import jclp.requireNotEmpty
import jclp.setting.Settings
import jclp.setting.getBoolean
import jclp.setting.getString
import jclp.vdm.VDM_DIRECTORY
import jem.Book
import jem.title
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Paths

const val EXT_EPM_METADATA = "jem.ext.meta"
const val MAKER_OVERWRITE_KEY = "maker.file.overwrite"

interface Parser {
    fun parse(input: String, arguments: Settings?): Book
}

interface Maker {
    fun make(book: Book, output: String, arguments: Settings?)
}

interface EpmFactory : KeyedService {
    val hasMaker get() = maker != null

    val maker: Maker? get() = null

    val hasParser get() = parser != null

    val parser: Parser? get() = null
}

object EpmManager : ServiceManager<EpmFactory>(EpmFactory::class.java) {
    fun getMaker(name: String) = get(name)?.maker

    fun getParser(name: String) = get(name)?.parser

    fun readBook(param: ParserParam): Book? =
            getParser(requireNotEmpty(param.epmName) { "Not found epm format" })?.parse(param.path, param.arguments)

    fun writeBook(param: MakerParam): String? {
        val epmName = requireNotEmpty(param.epmName) { "Not found epm format" }
        val maker = getMaker(epmName) ?: return null
        var path = Paths.get(param.path)
        if (path.isDirectory && (maker !is VdmMaker || param.arguments?.getString(MAKER_VDM_TYPE_KEY) != VDM_DIRECTORY)) {
            path = path.resolve("${param.book.title}.$epmName")
        }
        val output = path.toString()
        if (path.exists && param.arguments?.getBoolean(MAKER_OVERWRITE_KEY) != true) {
            throw FileAlreadyExistsException(output)
        }
        param.actualPath = output
        maker.make(param.book, output, param.arguments)
        return output
    }
}
