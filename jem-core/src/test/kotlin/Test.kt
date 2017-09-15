import jclp.log.Log
import jclp.log.LogLevel
import jclp.setting.Settings
import jem.Book
import jem.epm.EpmManager
import jem.epm.ParserParam
import jem.epm.ReusableParser


fun main(args: Array<String>) {
    Log.level = LogLevel.ALL
    println(EpmManager.readBook(ParserParam("E:/tmp/1/fh.demo")))
}