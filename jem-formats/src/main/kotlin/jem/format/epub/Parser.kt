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

import jclp.setting.Settings
import jclp.vdm.VdmEntry
import jclp.vdm.VdmReader
import jclp.vdm.readText
import jem.Book
import jem.epm.VdmParser
import jem.format.util.failParser
import jem.format.util.xmlAttribute
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

internal object EpubParser : VdmParser {
    override fun parse(input: VdmReader, arguments: Settings?) = if (input.readText(EPUB.MIME_PATH) != EPUB.MIME_EPUB) {
        failParser("epub.parse.badMime", EPUB.MIME_PATH, EPUB.MIME_EPUB)
    } else Book().apply {
        val data = Local(this, input, arguments)
        try {
            parseOpf(data)
            parseNcx(data)
        } catch (e: Exception) {
            cleanup()
            throw e
        }
    }

    private fun parseOpf(data: Local) {

    }

    private fun parseNcx(data: Local) {
        val xpp = data.newXpp()
    }

    private data class Local(val book: Book, val reader: VdmReader, val settings: Settings?) {
        lateinit var xpp: XmlPullParser

        lateinit var entry: VdmEntry

        fun getAttribute(name: String) = xmlAttribute(xpp, name, entry)

        fun newXpp(): XmlPullParser {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            xpp = factory.newPullParser()
            return xpp
        }

        fun openStream(path: String): InputStream {
            entry = reader.getEntry(path) ?: failParser("epub.parse.notFoundFile", path, reader.name)
            return reader.getInputStream(entry)
        }
    }
}
