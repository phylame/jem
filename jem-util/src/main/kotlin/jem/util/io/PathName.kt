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

package jem.util.io

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.spi.FileTypeDetector

const val UNKNOWN_MIME = "application/octet-stream"

fun String.slashify() = this.replace('\\', '/')

fun splitPath(path: String): Pair<Int, Int> {
    val length = path.length
    var end = length
    var start = 0
    loop@ for (i in length - 1 downTo 0) {
        when (path[i]) {
            '.' -> end = i
            '/', '\\' -> {
                start = i + 1
                break@loop
            }
        }
    }
    return start to end
}

fun baseName(path: String) = with(splitPath(path)) {
    path.substring(first, second)
}

fun dirName(path: String) = with(splitPath(path).first) {
    path.substring(0, this)
}

fun fullName(path: String) = with(splitPath(path).first) {
    if (this != 0) path.substring(this) else path
}

fun extName(path: String) = with(splitPath(path).second) {
    if (this != path.length) path.substring(this + 1) else ""
}

fun mimeType(path: String) = Files.probeContentType(Paths.get(path)) ?: UNKNOWN_MIME

class LocalMimeDetector : FileTypeDetector() {
    private val mimes = loadProperties("!jem/util/io/mime.properties")!!

    override fun probeContentType(path: Path): String?
            = mimes.getProperty(extName(path.toString()))
}
