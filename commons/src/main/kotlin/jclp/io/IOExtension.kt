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

package jclp.io

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneId

val Path.exists inline get() = Files.exists(this)

val Path.notExists inline get() = Files.notExists(this)

val Path.isDirectory inline get() = Files.isDirectory(this)

val Path.isNotDirectory inline get() = !Files.isDirectory(this)

val Path.lastModified: FileTime inline get() = Files.getLastModifiedTime(this)

val Path.size inline get() = Files.size(this)

fun FileTime.toLocalDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(toInstant(), ZoneId.systemDefault())

fun printableSize(size: Long): String {
    val format = NumberFormat.getNumberInstance()
    format.maximumFractionDigits = 2
    return when {
        size > 0x4000_0000 -> "${format.format(size.toDouble() / 0x4000_0000)} GB"
        size > 0x10_0000 -> "${format.format(size.toDouble() / 0x10_0000)} MB"
        size > 0x400 -> "${format.format(size.toDouble() / 0x400)} KB"
        else -> "$size B"
    }
}
