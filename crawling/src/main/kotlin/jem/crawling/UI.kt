package jem.crawling

import jem.crawler.CrawlerManager
import jem.epm.EpmManager
import jclp.log.Log
import jclp.util.CollectionUtils
import qaf.core.App
import qaf.ixin.IForm
import qaf.ixin.addGroupedComponents
import qaf.ixin.title
import qaf.ixin.x
import qaf.swing.*
import java.awt.Desktop
import java.awt.Insets
import java.awt.event.*
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.util.*
import javax.swing.*

fun note(title: String, message: Any, type: Int) {
    JOptionPane.showMessageDialog(Crawler.form, message, title, type)
}

fun confirm(title: String, message: Any): Boolean {
    return JOptionPane.showConfirmDialog(Crawler.form, message, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION
}

object Form : IForm("PW Crawler ${App.assembly.version}"), ActionListener {
    private const val TAG = "Forms"

    private lateinit var tfUrl: JTextField
    private lateinit var cbFormat: JComboBox<*>
    private lateinit var tfOutput: JTextField

    private lateinit var btnSearch: JButton
    private lateinit var btnOutput: JButton

    private lateinit var btnInfo: JButton
    private lateinit var btnClean: JButton
    private lateinit var btnStart: JButton
    private lateinit var btnMore: JButton
    private lateinit var cbBackup: JCheckBox

    private lateinit var taConsole: JTextArea

    fun print(text: String) {
        taConsole.append(text)
        taConsole.caretPosition = taConsole.document.length
    }

    internal fun setStartIcon() {
        btnStart.toolTipText = "开始下载电子书"
        try {
            btnStart.icon = ImageIcon(resourceFor("start.png"))
        } catch (e: MalformedURLException) {
            Log.e(TAG, e)
            btnStart.text = "开始"
        }
    }

    private fun setStopIcon() {
        btnStart.toolTipText = "停止操作"
        try {
            btnStart.icon = ImageIcon(resourceFor("stop.png"))
        } catch (e: MalformedURLException) {
            Log.e(TAG, e)
            btnStart.text = "停止"
        }
    }

    private fun downloadFile() {
        val app = Crawler
        if (app.stopTasks()) {
            app.log("已经取消操作\n")
            setStartIcon()
            return
        }
        val url = tfUrl.text.trim()
        if (url.isEmpty()) {
            note("下载电子书", "请输入链接地址", JOptionPane.ERROR_MESSAGE)
            tfUrl.requestFocus()
            return
        }
        val path = tfOutput.text
        if (path.isBlank()) {
            note("下载电子书", "请选择保存路径", JOptionPane.ERROR_MESSAGE)
            tfOutput.requestFocus()
            return
        }
        app.fetchBook(url, path, (cbFormat.selectedItem as Item).name, cbBackup.isSelected)
        setStopIcon()
    }

    private val fileChooser = JFileChooser()

    private fun selectFile() {
        fileChooser.dialogTitle = "选择保存位置"
        var path = tfOutput.text
        if (path.isNotBlank()) {
            if (path.matches("[\\w]:".toRegex())) { // windows driver
                path += "\\"
            }
            val file = File(path)
            val dir = if (!file.isDirectory) file.parentFile else file
            if (dir != null && dir.exists()) {
                fileChooser.currentDirectory = dir
                if (dir !== file) {
                    fileChooser.selectedFile = file
                }
            }
        }
        if (fileChooser.showSaveDialog(Crawler.form) == JFileChooser.APPROVE_OPTION) {
            tfOutput.text = fileChooser.selectedFile.path
        }
    }

    private fun searchBook() {
        note("搜索电子书", "功能正在开发中:)", JOptionPane.INFORMATION_MESSAGE)
    }

    private fun aboutApp() {
        val pane = JPanel()
        pane.layout = BoxLayout(pane, BoxLayout.PAGE_AXIS)
        for (host in CrawlerManager.knownHosts()) {
            val label = JLabel("<html>&nbsp;<a href='$host'>$host</a></html>")
            label.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    try {
                        Desktop.getDesktop().browse(URI(host))
                    } catch (ignored: IOException) {
                    } catch (ignored: URISyntaxException) {
                    }

                }
            })
            pane.add(label)
        }
        val message = arrayOf<Any>("支持的网址：", pane)
        note("PW Crawler", message, JOptionPane.PLAIN_MESSAGE)
    }

    override fun actionPerformed(e: ActionEvent) {
        val src = e.source
        if (src === btnStart || src === tfUrl || src === tfOutput) {
            downloadFile()
        } else if (src === btnClean) {
            taConsole.text = null
        } else if (src === btnInfo) {
            aboutApp()
        } else if (src === btnOutput) {
            selectFile()
        } else if (src === btnSearch) {
            searchBook()
        } else if (src === btnMore) {
            val path = tfOutput.text
            if (path.isNotBlank()) {
                try {
                    Desktop.getDesktop().browse(File(path).toURI())
                } catch (ex: IOException) {
                    Log.e(TAG, ex)
                }
            }
        }
    }

    init {
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                Crawler.exit()
            }
        })
        setupUI()
        isResizable = false
        iconImage = ImageIcon(resourceFor("icon.png")).image
    }

    private fun setupUI() {
        val margin = 5
        val etchedBorder = BorderFactory.createEtchedBorder()
        val emptyBorder = BorderFactory.createEmptyBorder(margin, margin, margin, margin)

        contentPane = pane(false) {
            border = emptyBorder
            boxLayout(BoxLayout.PAGE_AXIS) {
                val rigidO = 0 x margin

                pane {
                    border = BorderFactory.createEmptyBorder(2, margin, 2, margin) + etchedBorder

                    var lbUrl: JLabel? = null
                    var lbFormat: JLabel? = null
                    var lbOutput: JLabel? = null
                    addGroupedComponents(this, 3, 2, margin, 1,
                            label(false) {
                                lbUrl = this
                                title = "链接(&U):"
                            },
                            pane(false) {
                                boxLayout(BoxLayout.LINE_AXIS) {
                                    textField {
                                        tfUrl = this
                                        lbUrl?.labelFor = this
                                        addActionListener(this@Form)
                                    }
                                    button {
                                        btnSearch = this
                                        init("search.png", "在线搜索电子书")
                                    }
                                }
                            },
                            label(false) {
                                lbFormat = this
                                title = "格式(&F):"
                            },
                            comboBox<JPanel, Item>(false) {
                                cbFormat = this
                                lbFormat?.labelFor = this
                                var index = 0
                                var selected = -1
                                for (name in EpmManager.supportedMakers()) {
                                    addItem(Item(name))
                                    if (name == "epub") {
                                        selected = index
                                    }
                                    ++index
                                }
                                selectedIndex = selected
                            },
                            label(false) {
                                lbOutput = this
                                title = "路径(&O):"
                            },
                            pane(false) {
                                boxLayout(BoxLayout.LINE_AXIS) {
                                    textField {
                                        tfOutput = this
                                        lbOutput?.labelFor = this
                                        addActionListener(this@Form)
                                        text = System.getProperty("user.dir")
                                    }
                                    button {
                                        btnOutput = this
                                        init("open.png", "选择保存位置")
                                    }
                                }
                            })
                }

                add(Box.createRigidArea(rigidO))

                pane {
                    border = emptyBorder + etchedBorder
                    boxLayout(BoxLayout.LINE_AXIS) {
                        val rigidI = margin x 0

                        button {
                            btnInfo = this
                            init("info.png", "查看应用信息")
                        }

                        add(Box.createRigidArea(rigidI))

                        button {
                            btnClean = this
                            init("clean.png", "清空输出日志")
                        }

                        add(Box.createHorizontalGlue())

                        button {
                            btnStart = this
                            init("start.png", "开始下载电子书")
                        }

                        add(Box.createRigidArea(rigidI))

                        button {
                            btnMore = this
                            init("more.png", "打开保存目录")
                        }

                        add(Box.createRigidArea(rigidI))

                        checkBox {
                            cbBackup = this
                            isSelected = true
                            title = "备份(&B)"
                            toolTipText = "备份电子书为 PMAB 文件"
                        }
                    }
                }

                add(Box.createRigidArea(rigidO))

                pane {
                    border = etchedBorder
                    borderLayout {
                        center = scrollPane(false) {
                            border = BorderFactory.createTitledBorder("输出日志")
                            setViewportView(textArea(false) {
                                taConsole = this
                                rows = 8
                                columns = 45
                                isEditable = false
                            })
                        }
                    }
                }
            }
        }

        pack()
        setLocationRelativeTo(null)
    }

    fun resourceFor(name: String): URL = Crawler::class.java.getResource("/jem/crawling/$name")

    private fun JButton.init(icon: String, tip: String) {
        toolTipText = tip
        isBorderPainted = false
        margin = Insets(0, 0, 0, 0)
        this.icon = ImageIcon(resourceFor(icon))
        addActionListener(this@Form)
    }

    private class Item(val name: String) {
        val message = prop.getProperty(name) ?: name.toUpperCase()

        override fun toString(): String = message

        companion object {
            private val prop: Properties by lazy {
                CollectionUtils.propertiesFor("!jem/crawling/format-names.properties") ?: Properties()
            }
        }
    }
}
