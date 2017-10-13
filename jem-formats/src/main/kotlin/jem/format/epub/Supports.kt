package jem.format.epub

import jem.epm.EpmFactory
import jem.epm.Maker
import jem.epm.Parser
import java.nio.file.Path

internal val Path.vdmPath
    get() = normalize().toString().replace('\\', '/')

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

class EpubFactory : EpmFactory {
    override val keys = setOf("epub")

    override val name = "Epub for Jem"

    override val hasMaker = true

    override val maker: Maker = EpubMaker

    override val hasParser = true

    override val parser: Parser = EpubParser
}
