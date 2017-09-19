package jem.format.epub

import jem.epm.EpmFactory
import jem.epm.Maker
import jem.epm.Parser

internal const val MIME_PATH = "mimetype"
internal const val MIME_EPUB = "application/epub+zip"
internal const val MIME_NCX = "application/x-dtbncx+xml"

internal const val CONTAINER_PATH = "META-INF/container.xml"

class EpubFactory : EpmFactory {
    override val keys = setOf("epub")

    override val name = "Epub for Jem"

    override val maker: Maker = EpubMaker

    override val parser: Parser = EpubParser
}
