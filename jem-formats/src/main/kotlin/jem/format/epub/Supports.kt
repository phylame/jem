package jem.format.epub

import jclp.log.LogFacade
import jclp.log.LogLevel
import jem.epm.EpmFactory
import jem.epm.Maker
import jem.epm.Parser
import org.slf4j.LoggerFactory
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

    override val maker: Maker = EpubMaker

    override val parser: Parser = EpubParser
}

object SLF4JFacade : LogFacade {
    override fun log(tag: String, level: LogLevel, msg: String) {
        val logger = LoggerFactory.getLogger(tag)
        when (level) {
            LogLevel.DEBUG -> logger.debug(msg)
            LogLevel.TRACE -> logger.trace(msg)
            LogLevel.ERROR -> logger.error(msg)
            LogLevel.INFO -> logger.info(msg)
            LogLevel.WARN -> logger.warn(msg)
            else -> Unit
        }
    }

    override fun log(tag: String, level: LogLevel, msg: String, t: Throwable) {
        val logger = LoggerFactory.getLogger(tag)
        when (level) {
            LogLevel.DEBUG -> logger.debug(msg, t)
            LogLevel.TRACE -> logger.trace(msg, t)
            LogLevel.ERROR -> logger.error(msg, t)
            LogLevel.INFO -> logger.info(msg, t)
            LogLevel.WARN -> logger.warn(msg, t)
            else -> Unit
        }
    }
}
