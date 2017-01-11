package jem.crawling

import pw.phylame.qaf.core.App
import pw.phylame.qaf.ixin.IForm
import pw.phylame.ycl.io.IOUtils
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.ImageIcon
import javax.swing.WindowConstants

object MainForm : IForm("PW Crawling ${App.assembly.version}") {
    val board = Board()

    init {
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                Crawling.exit()
            }
        })
        iconImage = ImageIcon(IOUtils.resourceFor("!jem/crawling/icon.png")).image
        board.init()
        contentPane.add(board.root, BorderLayout.CENTER)
        pack()
        setLocationRelativeTo(null)
        isResizable = false
    }
}
