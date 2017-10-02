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

import jclp.io.extName
import jclp.setting.Settings
import jem.Book
import java.io.File

data class ParserParam(val path: String, val format: String = "", val arguments: Settings? = null) {
    val epmName get() = format.takeIf(String::isNotEmpty) ?: extName(File(path).canonicalPath)

    override fun toString(): String {
        return "ParserParam(path='$path', format='$format', epmName='$epmName', arguments=$arguments)"
    }
}

data class MakerParam(val book: Book, val path: String, val format: String = "", val arguments: Settings? = null) {
    val epmName get() = format.takeIf(String::isNotEmpty) ?: extName(File(path).canonicalPath)

    override fun toString(): String {
        return "MakerParam(book=$book, path='$path', format='$format', epmName='$epmName', arguments=$arguments)"
    }
}
