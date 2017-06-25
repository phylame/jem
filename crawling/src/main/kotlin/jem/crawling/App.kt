@file:JvmName("CrawlingKt")

package jem.crawling

import jem.core.Attributes
import jem.core.Book
import jem.core.Chapter

import jem.crawler.CrawlerConfig
import jem.crawler.CrawlerListener
import jem.crawler.CrawlerManager

import jem.epm.EpmManager
import jem.kotlin.title
import jem.util.JemException
import jem.util.Variants
import jem.util.text.Text
import jclp.log.Log
import jclp.util.DateUtils
import jclp.util.StringUtils
import qaf.core.App
import qaf.ixin.IDelegate
import qaf.ixin.Ixin
import rx.Observable
import rx.Observer
import rx.Subscriber
import rx.Subscription
import rx.schedulers.Schedulers
import rx.schedulers.SwingScheduler
import java.awt.Font
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import javax.swing.JOptionPane

object Crawler : IDelegate<Form>(), CrawlerListener {
    override fun onStart() {
        super.onStart()
        System.setProperty(EpmManager.AUTO_LOAD_KEY, "true")
        System.setProperty(CrawlerManager.AUTO_LOAD_KEY, "true")
    }

    override fun createForm(): Form {
        Ixin.isAntiAliasing = true
        Ixin.lafTheme = System.getProperty("crawling.theme", "Nimbus")
        Ixin.globalFont = Font.getFont("crawling.font", Font(Font.SANS_SERIF, Font.PLAIN, 14))
        Form.isVisible = true
        return Form
    }

    val isDone: Boolean get() = subscription?.isUnsubscribed ?: true

    fun exit() {
        if (!isDone && !confirm("退出", "当前任务未完成，继续退出？")) {
            return
        }
        App.exit(0)
    }

    override fun onQuit() {
        stopTasks()
        super.onQuit()
    }

    fun stopTasks(): Boolean {
        form.setStartIcon()
        val book = book
        if (book != null) {
            threadPool.submit {
                book.cleanup()
            }
            this.book = null
        }
        val subscription = subscription
        if (subscription != null) {
            subscription.unsubscribe()
            this.subscription = null
            return true
        } else {
            return false
        }
    }

    fun log(msg: String) {
        form.print(makeText(msg))
    }

    fun makeText(msg: String): String {
        return "[${DateUtils.format(Date(), "hh:mm:ss")}] $msg"
    }

    var book: Book? = null
    var subscription: Subscription? = null
    lateinit var subscriber: Subscriber<in String>
    private val threadPool = Executors.newFixedThreadPool(4);
    private val crawlerArgs = mapOf("crawler.parse.${CrawlerConfig.CRAWLER_LISTENER}" to this)

    fun fetchBook(url: String, output: String, format: String, backup: Boolean) {
        log("读取详情链接：$url\n")
        subscription = Observable.create<String> {
            subscriber = it
            var file = File(output)
            if (!file.isDirectory && file.exists()) {
                it.onError(IOException("文件已存在：$output"))
            }
            val book = EpmManager.readBook(url, "crawler", crawlerArgs)
            this.book = book
            if (file.isDirectory) {
                file = File(file, "${book.title}.${EpmManager.extensionsOfName(format)?.get(0) ?: format}")
                if (file.exists()) {
                    it.onError(IOException("文件已存在：${file.path}"))
                }
            }
            it.onNext("写入小说到文件: ${file.path}\n")
            EpmManager.writeBook(book, file, format, null)
            if (backup && format != "pmab") {
                file = File("${file.path}.pmab")
                EpmManager.writeBook(book, file, EpmManager.PMAB, null)
                it.onNext("备份小说到：$file\n")
            }
            it.onCompleted()
        }.subscribeOn(Schedulers.io())
                .observeOn(SwingScheduler.getInstance())
                .subscribe(object : Observer<String> {
                    override fun onError(e: Throwable) {
                        val msg = when (e) {
                            is JemException -> e.message
                            is IOException -> e.message
                            else -> {
                                Log.e("Crawling", e)
                                return
                            }
                        }
                        if (msg != null) {
                            note("保存小说失败", msg, JOptionPane.ERROR_MESSAGE)
                        } else {
                            Log.e("Crawling", "no message found for error")
                        }
                        stopTasks()
                    }

                    override fun onNext(o: String) {
                        log(o)
                    }

                    override fun onCompleted() {
                        log("完成！\n")
                        stopTasks()
                    }
                })
    }


    override fun fetchingText(total: Int, current: Int, chapter: Chapter) {
        subscriber.onNext("$current/$total: ${chapter.title}\n")
    }

    override fun attributeFetched(book: Book) {
        val b = StringBuilder("已获取电子书\n")
        val sep = StringUtils.multiplyOf("-", 81)
        b.append(sep).append("\n")
        for ((key, value) in book.attributes.entries()) {
            b.append(" - ").append(Attributes.titleOf(key)).append("：")
            when (value) {
                is Text -> {
                    for ((i, line) in value.text.lines().withIndex()) {
                        b.append(if (i != 0) "     " else "").append(line).append("\n")
                    }
                }
                else -> b.append(Variants.printable(value)).append("\n")
            }
        }
        b.append(sep).append("\n")
        b.append(makeText("读取目录……\n"))
        subscriber.onNext(b.toString())
    }

    override fun contentsFetched(book: Book) {
        subscriber.onNext("获取目录成功\n")
    }
}

fun main(args: Array<String>) {
    App.run("crawling", "1.0", Crawler, args)
}
