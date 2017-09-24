package jem.format.epub

import jclp.log.LogFacade
import jclp.log.LogLevel
import jem.epm.EpmFactory
import jem.epm.Maker
import jem.epm.Parser
import org.slf4j.LoggerFactory
import java.nio.file.Path

internal const val MIME_PATH = "mimetype"
internal const val MIME_EPUB = "application/epub+zip"
internal const val MIME_NCX = "application/x-dtbncx+xml"
internal const val MIME_OPF = "application/oebps-package+xml"
internal const val MIME_XHTML = "application/xhtml+xml"

internal const val DUOKAN_FULLSCREEN = "duokan-page-fullscreen"

internal const val BOOK_ID = "book-id"
internal const val COVER_ID = "cover"

internal val Path.vdmPath
    get() = normalize().toString().replace('\\', '/')

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
        }
    }
}
