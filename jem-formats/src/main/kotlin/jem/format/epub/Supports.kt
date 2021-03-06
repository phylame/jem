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

import jem.epm.EpmFactory
import jem.epm.FileParser
import jem.epm.Maker

internal object EPUB {
    const val MIME_PATH = "mimetype"

    const val MIME_EPUB = "application/epub+zip"
    const val MIME_NCX = "application/x-dtbncx+xml"
    const val MIME_OPF = "application/oebps-package+xml"
    const val MIME_XHTML = "application/xhtml+xml"
    const val MIME_CSS = "text/css"

    const val XMLNS_OPS = "http://www.idpf.org/2007/ops"
    const val XMLNS_OPF = "http://www.idpf.org/2007/opf"
    const val XMLNS_DCME = "http://purl.org/dc/elements/1.1/"
    const val XMLNS_NCX = "http://www.daisy.org/z3986/2005/ncx/"
    const val XMLNS_XHTML = "http://www.w3.org/1999/xhtml"

    const val MANIFEST_NAVIGATION = "nav"
    const val MANIFEST_COVER_IMAGE = "cover-image"

    const val SPINE_DUOKAN_FULLSCREEN = "duokan-page-fullscreen"
}

class EpubFactory : EpmFactory, FileParser {
    override val keys = setOf("epub")

    override val name = "Epub for Jem"

    override val hasMaker = true

    override val maker: Maker = EpubMaker

    override val hasParser = false
}
