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

import jclp.io.slashify
import jclp.setting.Settings
import jclp.vdm.VdmWriter
import jclp.vdm.useStream
import jclp.xml.xml
import jem.epm.EpmFactory
import jem.epm.FileParser
import jem.epm.Maker
import jem.epm.Parser
import jem.format.util.xmlEncoding
import jem.format.util.xmlIndent
import jem.format.util.xmlSeparator
import java.nio.charset.Charset
import java.nio.file.Path

internal val Path.vdmPath
    get() = normalize().toString().slashify()

internal object EPUB {
    const val MIME_PATH = "mimetype"
    const val MIME_EPUB = "application/epub+zip"
    const val MIME_NCX = "application/x-dtbncx+xml"
    const val MIME_OPF = "application/oebps-package+xml"
    const val MIME_XHTML = "application/xhtml+xml"

    const val DUOKAN_FULLSCREEN = "duokan-page-fullscreen"

    const val BOOK_ID = "book-id"
    const val COVER_ID = "cover"
}

class EpubFactory : EpmFactory, FileParser {
    override val keys = setOf("epub")

    override val name = "Epub for Jem"

    override val hasMaker = true

    override val maker: Maker = EpubMaker

    override val hasParser = true

    override val parser: Parser = EpubParser
}

private const val CONTAINER_PATH = "META-INF/container.xml"

fun writeContainer(writer: VdmWriter, settings: Settings?, files: Map<String, String>) {
    writer.useStream(CONTAINER_PATH) {
        it.writer(Charset.forName(settings.xmlEncoding)).xml(settings.xmlIndent, settings.xmlSeparator, settings.xmlEncoding) {
            tag("container") {
                attr["version"] = "1.0"
                attr["xmlns"] = "urn:oasis:names:tc:opendocument:xmlns:container"
                tag("rootfiles") {
                    for ((path, mime) in files) {
                        tag("rootfile") {
                            attr["full-path"] = path
                            attr["media-type"] = mime
                        }
                    }
                }
            }
        }
    }
}
