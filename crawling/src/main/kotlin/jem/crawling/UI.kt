package jem.crawling

import pw.phylame.qaf.ixin.IForm
import pw.phylame.qaf.ixin.IStatusBar
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

object Board : IForm("PW Crawling") {
    lateinit var outputPane: JTextArea

    init {
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                Crawling.exit()
            }
        })
        initComps()
    }

    private fun initComps() {
        val content = JPanel()

        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        splitPane.topComponent = content

        outputPane = JTextArea()
        outputPane.border = BorderFactory.createTitledBorder("Output")
        splitPane.bottomComponent = outputPane
        contentPane.add(splitPane, BorderLayout.CENTER)

        statusBar = IStatusBar()
        contentPane.add(statusBar, BorderLayout.PAGE_END)
        size = Dimension(800, 600)
        setLocationRelativeTo(null)
    }
}
