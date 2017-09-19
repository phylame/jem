package jem.format.epub

import jem.epm.EpmFactory
import jem.epm.Maker

internal const val MIME_PATH = "mimetype"
internal const val MIME_EPUB = "application/epub+zip"
internal const val MIME_NCX = "application/x-dtbncx+xml"

class EpubFactory : EpmFactory {
    override val keys = setOf("epub")

    override val name = "Epub for Jem"

    override val maker: Maker = EpubMaker
}