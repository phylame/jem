package jem.crawling

import pw.phylame.jem.epm.EpmManager
import pw.phylame.qaf.core.App
import pw.phylame.qaf.ixin.IDelegate
import pw.phylame.qaf.ixin.Ixin
import java.awt.Font

object Crawling : IDelegate<Board>() {
    override fun onStart() {
        super.onStart()
        System.setProperty(EpmManager.AUTO_LOAD_KEY, "true")
    }

    override fun createForm(): Board {
        Ixin.init(true, false, "Nimbus", Font.decode("Microsoft YaHei UI-PLAIN-14"))
        return Board
    }

    override fun onReady() {
        form.isVisible = true
        form.statusText = "Ready"
    }

    fun exit() {
        App.exit(0)
    }
}

fun main(args: Array<String>) {
    App.run("crawling", "1.0", args, Crawling)
}
