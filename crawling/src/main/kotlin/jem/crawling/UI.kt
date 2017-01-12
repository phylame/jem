package jem.crawling

import pw.phylame.qaf.core.App
import pw.phylame.qaf.ixin.IForm
import pw.phylame.qaf.ixin.Ixin
import pw.phylame.qaf.ixin.groupedPane
import pw.phylame.qaf.swing.*
import pw.phylame.ycl.io.IOUtils
import java.awt.BorderLayout
import java.awt.Font
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

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

fun createForm() = frame {
    contentPane = panel(false) {
        border = BorderFactory.createEmptyBorder(50, 50, 50, 50)

        boxLayout(BoxLayout.PAGE_AXIS) {

            add(groupedPane(3, 2, 5, 5,
                    JLabel("URL:"), JTextField(),
                    JLabel("Format:"), JComboBox<String>(),
                    JLabel("Output:"), panel(false) {
                borderLayout {
                    center = JTextField()
                    east = JButton("Select")
                }
            }
            ).apply {
                border = BorderFactory.createEtchedBorder()
            })

            panel {
                border = BorderFactory.createEtchedBorder()

                borderLayout {
                    west = panel(false) {
                        button {
                            text = "About"
                        }

                        button {
                            text = "Clean"
                        }
                    }

                    east = panel(false) {
                        button {
                            text = "Start"
                        }
                        button {
                            text = "More"
                        }

                        add(JCheckBox("Backup"))
                    }
                }
            }

            panel {
                border = BorderFactory.createEtchedBorder()

                borderLayout {
                    center = scrollPane(false) {
                        border = BorderFactory.createTitledBorder("Output Log")
                        setViewportView(JTextArea().apply {
                            rows = 8
                            columns = 47
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

fun main(args: Array<String>) {
    Ixin.init(true,
            false,
            System.getProperty("crawling.theme", "Nimbus"),
            Font.getFont("crawling.font", Font(Font.DIALOG, Font.PLAIN, 14)))
    createForm().isVisible = true
}
