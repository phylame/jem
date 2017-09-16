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

package jclp.io

import java.io.File

fun splitPath(path: String): Pair<Int, Int> {
    var seppos: Int
    var extpos = path.length
    var extFound = false
    seppos = extpos - 1
    while (seppos >= 0) {
        val ch = path[seppos]
        if (ch == '.' && !extFound) {
            extpos = seppos
            extFound = true
        } else if (ch == '/' || ch == '\\') {
            break
        }
        --seppos
    }
    return seppos to extpos
}

fun baseName(path: String) = splitPath(path).let { path.substring(it.first + 1, it.second) }

fun dirName(path: String) = splitPath(path).first.let { if (it != -1) path.substring(0, it) else "" }

fun fullName(path: String) = splitPath(path).first.let { path.substring(if (it != 0) it + 1 else it) }

fun extName(path: String) = splitPath(path).second.let { if (it != path.length) path.substring(it + 1) else "" }

fun File.createDirectory() = exists() || mkdirs()
