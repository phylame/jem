package jem.epm

import jclp.ServiceManager
import jclp.ServiceProvider
import jclp.setting.Settings
import jem.Book

interface Parser {
    fun parse(input: String, arguments: Settings?): Book
}

interface Maker {
    fun make(book: Book, output: String, arguments: Settings?)
}

interface EpmFactory : ServiceProvider {
    fun hasMaker(): Boolean = maker != null

    val maker: Maker? get() = null

    fun hasParser(): Boolean = parser != null

    val parser: Parser? get() = null
}

interface ReusableParser : Parser, EpmFactory {
    override fun hasParser() = true

    override val parser get() = this
}

interface ReusableMaker : Maker, EpmFactory {
    override fun hasMaker() = true

    override val maker get() = this
}

object EpmManager : ServiceManager<EpmFactory>(EpmFactory::class.java) {
    fun getParser(name: String) = getService(name)?.parser

    fun getMaker(name: String) = getService(name)?.maker

    fun readBook(param: ParserParam) = getParser(param.epmName)?.parse(param.path, param.arguments)

    fun writeBook(param: MakerParam) = getMaker(param.epmName)?.let {
        it.make(param.book, param.output, param.arguments)
        true
    } ?: false
}
