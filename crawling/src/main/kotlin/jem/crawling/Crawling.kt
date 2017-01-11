package jem.crawling

import pw.phylame.jem.core.Attributes
import pw.phylame.jem.core.Book
import pw.phylame.jem.core.Chapter
import pw.phylame.jem.crawler.AbstractProvider
import pw.phylame.jem.crawler.CrawlerConfig
import pw.phylame.jem.crawler.OnFetchingListener
import pw.phylame.jem.crawler.ProviderManager
import pw.phylame.jem.epm.EpmManager
import pw.phylame.jem.title
import pw.phylame.jem.util.Variants
import pw.phylame.jem.util.text.Text
import pw.phylame.qaf.core.App
import pw.phylame.qaf.core.dumpToString
import pw.phylame.qaf.ixin.IDelegate
import pw.phylame.qaf.ixin.Ixin
import pw.phylame.ycl.util.DateUtils
import pw.phylame.ycl.util.StringUtils
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
import javax.swing.JOptionPane

object Crawling : IDelegate<MainForm>(), OnFetchingListener {
    override fun onStart() {
        super.onStart()
        System.setProperty(EpmManager.AUTO_LOAD_KEY, "true")
        System.setProperty(ProviderManager.AUTO_LOAD_KEY, "true")
    }

    override fun createForm(): MainForm {
        Ixin.init(true, false, System.getProperty("crawling.theme", "Nimbus"),
                Font.getFont("crawling.font", Font(Font.DIALOG, Font.PLAIN, 14))
        )
        MainForm.isVisible = true
        return MainForm
    }

    fun exit() {
        if (!(subscription?.isUnsubscribed ?: true)) {
            val i = JOptionPane.showConfirmDialog(form, "当前任务未完成，继续退出？", "退出",
                    JOptionPane.OK_CANCEL_OPTION)
            if (i != JOptionPane.OK_OPTION) {
                return
            }
        }
        stop()
        AbstractProvider.cleanup()
        App.exit(0)
    }

    fun stop(): Boolean {
        book?.cleanup()
        book = null
        if (subscription != null) {
            subscription!!.unsubscribe()
            subscription = null
            return true
        } else {
            return false
        }
    }

    fun echo(msg: String) {
        form.board.print(makeText(msg))
    }

    fun makeText(msg: String): String {
        return "[${DateUtils.format(Date(), "hh:mm:ss.SSS")}] $msg"
    }

    var book: Book? = null
    var subscription: Subscription? = null
    lateinit var subscriber: Subscriber<in String>

    fun fetchBook(url: String, output: String, format: String, backup: Boolean) {
        echo("读取详情链接：$url\n")
        val args = mapOf<String, Any>(
                "crawler.parse.${CrawlerConfig.FETCH_LISTENER}" to this
        )
        subscription = Observable.create<String> {
            subscriber = it
            var file = File(output)
            if (!file.isDirectory && file.exists()) {
                it.onError(IOException("文件已存在：$output"))
            }
            val book = EpmManager.readBook(url, "crawler", args)
            this.book = book
            if (file.isDirectory) {
                file = File(file, "${book.title}.${EpmManager.extensionsOfName(format)?.get(0) ?: format}")
                if (file.exists()) {
                    it.onError(IOException("文件已存在：${file.path}"))
                }
            }
            it.onNext("写入小说到文件……\n")
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
                        form.board.note("保存小说", e.message, JOptionPane.ERROR_MESSAGE)
                        form.board.print(e.dumpToString())
                        stop()
                        form.board.setStartIcon()
                    }

                    override fun onNext(o: String) {
                        echo(o)
                    }

                    override fun onCompleted() {
                        echo("完成！\n")
                        stop()
                        form.board.setStartIcon()
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
    App.run("crawling", "1.0", args, Crawling)
}
