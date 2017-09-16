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

import jclp.ServiceManager
import jclp.ServiceProvider
import jclp.setting.Settings
import jclp.setting.getBoolean
import jclp.setting.getString
import jem.Book
import jem.title
import java.io.File

interface Parser {
    fun parse(input: String, arguments: Settings?): Book
}

interface Maker {
    fun make(book: Book, output: String, arguments: Settings?)
}

interface EpmFactory : ServiceProvider {
    val hasMaker get() = maker != null

    val maker: Maker? get() = null

    val hasParser get() = parser != null

    val parser: Parser? get() = null
}

object EpmManager : ServiceManager<EpmFactory>(EpmFactory::class.java) {
    fun getMaker(name: String) = get(name)?.maker

    fun getParser(name: String) = get(name)?.parser

    fun readBook(param: ParserParam): Book? {
        val epmName = param.epmName
        require(epmName.isNotEmpty()) { "Not found epm format" }
        return getParser(epmName)?.parse(param.path, param.arguments)
    }

    fun writeBook(param: MakerParam): String? {
        val epmName = param.epmName
        require(epmName.isNotEmpty()) { "Not found epm format" }
        var file = File(param.path)
        val maker = getMaker(epmName) ?: return null
        if (file.isDirectory && (maker !is VDMMaker || param.arguments?.getString("maker.vdm.type") != "dir")) {
            file = File(file, "${param.book.title}.$epmName")
        }
        file.takeIf {
            it.exists() && param.arguments?.getBoolean("maker.file.overwrite") != true
        }?.let {
            throw FileAlreadyExistsException(it)
        }
        maker.make(param.book, file.path, param.arguments)
        return file.path
    }
}

fun Book.Companion.from(path: String, format: String = "", arguments: Settings? = null) = ParserParam(path, format, arguments).let {
    EpmManager.readBook(it)
}

fun Book.write(path: String, format: String = "", arguments: Settings? = null) = MakerParam(this, path, format, arguments).let {
    EpmManager.writeBook(it)
}
