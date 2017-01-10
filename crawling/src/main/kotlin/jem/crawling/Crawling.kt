package jem.crawling

import pw.phylame.jem.core.Attributes
import pw.phylame.jem.core.Book
import pw.phylame.jem.core.Chapter
import pw.phylame.jem.crawler.CrawlerConfig
import pw.phylame.jem.crawler.OnFetchingListener
import pw.phylame.jem.crawler.ProviderManager
import pw.phylame.jem.epm.EpmManager
import pw.phylame.jem.title
import pw.phylame.jem.util.Variants
import pw.phylame.jem.util.text.Text
import pw.phylame.qaf.core.App
import pw.phylame.qaf.ixin.IDelegate
import pw.phylame.qaf.ixin.Ixin
import pw.phylame.ycl.util.DateUtils
import pw.phylame.ycl.util.StringUtils
import rx.Observable
import rx.Observer
import rx.Subscriber
import rx.schedulers.Schedulers
import rx.schedulers.SwingScheduler
import java.awt.Font
import java.io.File
import java.net.MalformedURLException
import java.util.*

object Crawling : IDelegate<MainForm>(), OnFetchingListener {
    override fun onStart() {
        super.onStart()
        System.setProperty(EpmManager.AUTO_LOAD_KEY, "true")
        System.setProperty(ProviderManager.AUTO_LOAD_KEY, "true")
    }

    override fun createForm(): MainForm {
        Ixin.init(true,
                false,
                System.getProperty("crawling.theme", "Nimbus"),
                Font.getFont("crawling.font", Font(Font.DIALOG, Font.PLAIN, 14))
        )
        return MainForm
    }

    override fun onReady() {
        form.isVisible = true
        form.statusText = "Ready"
        form.board.redirectOutput()
    }

    fun exit() {
        App.exit(0)
    }

    fun echo(msg: String) {
        println(makeText(msg))
    }

    fun makeText(msg: String): String {
        return "[${DateUtils.format(Date(), "hh:mm:ss.SSS")}] $msg"
    }

    fun fetchBook(url: String, output: String, format: String) {
        echo("读取详情链接：$url")
        Observable.create<Any> {
            subscriber = it
            val args = mapOf<String, Any>(
                    "crawler.parse.${CrawlerConfig.FETCH_LISTENER}" to this
            )
            val book = EpmManager.readBook(url, "crawler", args)
            var file = File(output)
            if (file.isDirectory) {
                file = File(file, "${book.title}.${EpmManager.extensionsOfName(format)?.get(0) ?: format}")
            }
            EpmManager.writeBook(book, file, format, null)
            it.onCompleted()
        }.subscribeOn(Schedulers.io())
                .observeOn(SwingScheduler.getInstance())
                .subscribe(object : Observer<Any> {
                    override fun onError(e: Throwable) {
                        when (e) {
                            is MalformedURLException -> error("无效链接地址")
                        }
                    }

                    override fun onNext(o: Any) {
                        when (o) {
                            is String -> {
                                echo(o)
                            }
                            is Pair<*, *> -> {
                                println(o.first)
                            }
                        }
                    }

                    override fun onCompleted() {
                    }
                })
    }

    lateinit var subscriber: Subscriber<in Any>

    override fun fetchingText(total: Int, current: Int, chapter: Chapter) {
        subscriber.onNext("$current/$total: ${chapter.title}")
    }

    override fun attributeFetched(book: Book) {
        val b = StringBuilder("已获取电子书\n")
        for ((key, value) in book.attributes.entries()) {
            b.append(" - ").append(Attributes.titleOf(key)).append("=")
            when (value) {
                is Text -> {
                    for ((i, line) in value.text.lines().withIndex()) {
                        b.append(if (i != 0) "     " else "").append(line).append("\n")
                    }
                }
                else -> b.append(Variants.printable(value)).append("\n")
            }
        }
        b.append(StringUtils.multiplyOf("-", 47)).append("\n")
        b.append(makeText("读取目录……"))
        subscriber.onNext(b.toString())
    }

    override fun contentsFetched(book: Book) {
        subscriber.onNext("获取目录成功")
        subscriber.onNext("写入小说到文件……")
    }
}

fun main(args: Array<String>) {
    App.run("crawling", "1.0", args, Crawling)
}
