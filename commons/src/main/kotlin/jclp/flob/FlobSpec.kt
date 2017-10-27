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

package jclp.flob

import java.io.InputStream
import java.io.OutputStream

interface Flob {
    val name: String

    val mimeType: String

    fun openStream(): InputStream

    fun writeTo(output: OutputStream): Long {
        return openStream().use { it.copyTo(output) }
    }
}

open class FlobWrapper(val flob: Flob) : Flob {
    override val name = flob.name

    override val mimeType = flob.mimeType

    override fun openStream() = flob.openStream()

    override fun writeTo(output: OutputStream) = flob.writeTo(output)

    override fun equals(other: Any?) = flob == other

    override fun hashCode() = flob.hashCode()

    override fun toString() = flob.toString()
}
