/*
 * Copyright 2017 Peng Wan <phylame@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jem.imabw.app.ui

import jem.imabw.app.GOTO
import jem.imabw.app.Imabw
import org.jdesktop.swingx.JXLabel
import org.jdesktop.swingx.JXPanel
import qaf.core.App
import qaf.ixin.iconFor
import qaf.swing.label
import qaf.swing.north
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

object Dashboard : JXPanel(BorderLayout()) {
    init {
        north = label(false) {
            text = App.tr("welcome.text")
            horizontalAlignment = SwingConstants.CENTER
        }
    }
}

object Indicator : JXPanel() {
    private lateinit var ruler: JLabel
    private lateinit var words: JLabel
    private lateinit var readonly: JLabel

    init {
        initUI()
        resetEditor(false)
    }

    private fun initUI() {
        ruler = JXLabel()
        ruler.toolTipText = App.tr("status.ruler.tip")
        ruler.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (!ruler.isEnabled || e.isMetaDown) {
                    return
                }
                Imabw.performed(GOTO)
            }
        })

        words = JXLabel()
        words.toolTipText = App.tr("status.words.tip")

        readonly = JXLabel(iconFor("status/readwrite.png"))
        readonly.toolTipText = App.tr("status.readonly.tip")
        readonly.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (!readonly.isEnabled || e.isMetaDown) {
                    return
                }

//                val tab = viewer.tabbedEditor.activeTab
//                if (tab != null) {
//                    val editor = tab.editor
//                    editor.isReadonly = !editor.isReadonly
//                    setReadonly(editor.isReadonly)
//                }

            }
        })

        val message = JXLabel(iconFor("status/message.png"))
        message.toolTipText = App.tr("status.message.tip")
        message.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                // todo: show message window
            }
        })

        layout = BoxLayout(this, BoxLayout.LINE_AXIS)
        addComponents(
                JSeparator(JSeparator.VERTICAL), ruler,
                JSeparator(JSeparator.VERTICAL), words,
                JSeparator(JSeparator.VERTICAL), readonly,
                JSeparator(JSeparator.VERTICAL), message
        )
    }

    private fun addComponents(vararg components: Component) {
        for (com in components) {
            add(com)
            add(Box.createRigidArea(Dimension(5, 0)))
        }
    }

    fun setRuler(row: Int, column: Int, selected: Int) {
        val b = StringBuilder()
        if (row < 0) {      // invalid
            b.append("n/a")
        } else {
            b.append(row).append(":").append(column)
            if (selected > 0) {
                b.append("/").append(selected)
            }
        }
        ruler.text = b.toString()
    }

    fun setWords(n: Int) {
        if (n < 0) {        // invalid
            words.text = "n/a"
        } else {
            words.text = n.toString()
        }
    }

    fun setReadonly(readonly: Boolean) {
        if (readonly) {
            this.readonly.icon = iconFor("status/readonly.png")
        } else {
            this.readonly.icon = iconFor("status/readwrite.png")
        }
    }

    fun message(msg: String) {

    }

    fun resetEditor(enable: Boolean) {
        if (!enable) {
            setRuler(-1, -1, -1)
            setWords(-1)
        }
        ruler.isEnabled = enable
        words.isEnabled = enable
        readonly.isEnabled = enable
    }

}
