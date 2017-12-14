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

package jem.format.epub

import jclp.io.Flob
import jclp.vdm.VdmWriter
import jclp.vdm.writeFlob

fun opsPathOf(name: String) = "OEBPS/$name"

private fun classifyPath(name: String, mime: String) = when {
    mime.startsWith("text/") -> "Text/$name"
    mime.startsWith("image/") -> "Images/$name"
    mime == "Text/css" -> "styles/$name"
    else -> "Extra/$name"
}

fun VdmWriter.writeToOps(flob: Flob, name: String): String {
    val path = classifyPath(name, flob.mimeType)
    writeFlob(opsPathOf(path), flob)
    return path
}
