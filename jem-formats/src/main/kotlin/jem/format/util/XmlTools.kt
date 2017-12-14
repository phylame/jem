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

package jem.format.util

import jclp.setting.Settings
import jclp.vdm.VdmWriter
import jclp.vdm.useStream
import jclp.xml.XML
import jclp.xml.newSerializer
import jclp.xml.startDocument
import jclp.xml.xml
import org.xmlpull.v1.XmlSerializer
import java.io.OutputStream
import java.io.Writer
import java.nio.charset.Charset

fun newSerializer(output: Writer, settings: Settings?): XmlSerializer =
        newSerializer(output, settings.xmlIndent, settings.xmlSeparator)

fun newSerializer(output: OutputStream, settings: Settings?): XmlSerializer =
        newSerializer(output, settings.xmlEncoding, settings.xmlIndent, settings.xmlSeparator)

inline fun VdmWriter.newSerializer(entry: String, settings: Settings?, block: XmlSerializer.() -> Unit) {
    useStream(entry) {
        with(newSerializer(it, settings)) {
            startDocument(settings.xmlEncoding)
            block.invoke(this)
            endDocument()
        }
    }
}

inline fun xmlDsl(output: Writer, settings: Settings?, block: XML.() -> Unit) {
    output.xml(settings.xmlIndent, settings.xmlSeparator, settings.xmlEncoding, block = block)
}

inline fun xmlDsl(output: OutputStream, settings: Settings?, block: XML.() -> Unit) {
    xmlDsl(output.bufferedWriter(Charset.forName(settings.xmlEncoding)), settings, block)
}

inline fun VdmWriter.xmlDsl(entry: String, settings: Settings?, block: XML.() -> Unit) {
    useStream(entry) { xmlDsl(it, settings, block) }
}
