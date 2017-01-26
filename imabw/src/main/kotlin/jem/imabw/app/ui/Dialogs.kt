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

/*
 * C()opyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Imabw.
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

import com.toedter.calendar.JCalendar
import com.toedter.components.JLocaleChooser
import jem.core.Book
import jem.core.Chapter
import pw.phylame.commons.io.PathUtils
import pw.phylame.qaf.core.App
import pw.phylame.qaf.core.dump
import pw.phylame.qaf.core.tr
import pw.phylame.qaf.ixin.*
import java.awt.*
import java.awt.event.ActionEvent
import java.io.File
import java.net.URI
import java.text.NumberFormat
import java.text.ParseException
import java.util.*
import javax.swing.*
import javax.swing.filechooser.FileFilter
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.JTextComponent
import javax.swing.text.NumberFormatter

abstract class BaseDialog<R> : IResultfulDialog<R> {
    companion object {
        val BUTTON_OK = Item("d.common.buttonOk")
        val BUTTON_CANCEL = Item("d.common.buttonCancel")
        val BUTTON_CLOSE = Item("d.common.buttonClose")
    }

    constructor(owner: Frame, title: String, modal: Boolean = false) : super(owner, title, modal)

    constructor(owner: Dialog, title: String, modal: Boolean = false) : super(owner, title, modal)

    override fun createControlsPane(alignment: Int, vararg components: Component): JPanel? {
        val pane = super.createControlsPane(alignment, *components)
        if (pane != null) {
            pane.background = pane.background.darker()
        }
        return pane
    }
}

enum class MessageStyle(val icon: String, val decoration: Int) {
    Alert("alert", JRootPane.ERROR_DIALOG),
    Information("information", JRootPane.INFORMATION_DIALOG),
    Prohibit("prohibit", JRootPane.ERROR_DIALOG),
    Question("question", JRootPane.QUESTION_DIALOG),
    Success("success", JRootPane.INFORMATION_DIALOG),
    Warning("warning", JRootPane.WARNING_DIALOG),
    Save("save", JRootPane.QUESTION_DIALOG),
    Plain("", JRootPane.PLAIN_DIALOG),
    None("", JRootPane.NONE)
}

internal class MessageDialog : IOptionDialog {
    companion object {
        fun create(parent: Component?, title: String, message: Any, style: MessageStyle): MessageDialog {
            val dialog = createDialog(parent, title, true, MessageDialog::class.java)
            dialog.style = style
            dialog.message = message
            return dialog
        }
    }

    var style: MessageStyle = MessageStyle.None
        set(value) {
            if (value.icon.isNotEmpty()) {
                icon = iconFor(":dialog/" + value.icon + ".png")
            }
            setDecorationIfNeed(value.decoration)
            field = value
        }

    constructor(owner: Frame, title: String, modal: Boolean = true) : super(owner, title, modal)

    constructor(owner: Dialog, title: String, modal: Boolean = true) : super(owner, title, modal)

    override fun createControlsPane(alignment: Int, vararg components: Component): JPanel? {
        val pane = super.createControlsPane(alignment, *components)
        if (pane != null) {
            pane.background = pane.background.darker()
        }
        return pane
    }

    fun setRightAlignedOptions(defaultOption: Int, vararg options: Any) {
        setOptions(SwingConstants.RIGHT, defaultOption, *options)
    }

    fun addCloseButton(alignment: Int) {
        setOptions(alignment, 0, BaseDialog.BUTTON_OK)
    }
}

internal abstract class InputDialog<D, T : JTextComponent> : BaseDialog<D> {
    protected lateinit var texting: T
    protected lateinit var buttonOk: JButton

    lateinit var tipText: String
    var initial: D? = null
    var requireChange: Boolean = false
    var canEmpty: Boolean = false

    protected var inputted = false

    constructor(owner: Frame, title: String, modal: Boolean) : super(owner, title, modal) {
        setDecorationIfNeed(JRootPane.QUESTION_DIALOG)
    }

    constructor(owner: Dialog, title: String, modal: Boolean) : super(owner, title, modal) {
        setDecorationIfNeed(JRootPane.QUESTION_DIALOG)
    }
}

internal class SimpleInputDialog : InputDialog<Any, JTextField> {
    var formatter: JFormattedTextField.AbstractFormatter? = null

    constructor(owner: Frame, title: String, modal: Boolean) : super(owner, title, modal) {
        setDecorationIfNeed(JRootPane.QUESTION_DIALOG)
    }

    constructor(owner: Dialog, title: String, modal: Boolean) : super(owner, title, modal) {
        setDecorationIfNeed(JRootPane.QUESTION_DIALOG)
    }

    override val result: Any?
        get() = if (!inputted) {
            null
        } else if (formatter != null) {
            (texting as JFormattedTextField).value
        } else {
            texting.text
        }

    override fun createComponents(userPane: JPanel) {
        val okAction = object : IAction(BUTTON_OK.id) {
            override fun actionPerformed(e: ActionEvent) {
                inputted = true
                dispose()
            }
        }
        buttonOk = JButton(okAction)
        defaultButton = buttonOk
        createTextField(formatter)

        userPane.add(texting.labelled(tipText), BorderLayout.CENTER)
        userPane.add(texting, BorderLayout.PAGE_END)

        controlsPane = createControlsPane(SwingConstants.RIGHT, buttonOk, createCloseButton(BUTTON_CANCEL.id))
    }

    private fun createTextField(formatter: JFormattedTextField.AbstractFormatter?) {
        if (formatter == null) {
            texting = JTextField()
            if (initial != null) {
                texting.text = initial!!.toString()
            }
            buttonOk.isEnabled = initial != null && initial != "" && !requireChange
        } else {
            val formattedTextField = JFormattedTextField(initial)
            formattedTextField.formatterFactory = DefaultFormatterFactory(formatter)
            texting = formattedTextField
            this.formatter = formatter
        }
        texting.columns = 32
        texting.selectAll()
        buttonOk.isEnabled = !requireChange && (canEmpty || "" != initial)

        val doc = texting.document
        texting.addCaretListener { e ->
            buttonOk.isEnabled = if (doc.length === 0) { // empty
                canEmpty && (!requireChange || "" != initial)
            } else if (requireChange) { // not empty
                initial == null || isChanged
            } else {
                true
            }
        }
    }

    // invalid input
    private val isChanged: Boolean
        get() {
            if (formatter == null) {
                return initial != texting.text
            } else {
                try {
                    return initial != formatter!!.stringToValue(texting.text)
                } catch (e: ParseException) {
                    return false
                }
            }
        }
}

internal class LongInputDialog : InputDialog<String, JTextArea> {

    constructor(owner: Frame, title: String, modal: Boolean) : super(owner, title, modal) {
        setDecorationIfNeed(JRootPane.QUESTION_DIALOG)
    }

    constructor(owner: Dialog, title: String, modal: Boolean) : super(owner, title, modal) {
        setDecorationIfNeed(JRootPane.QUESTION_DIALOG)
    }

    override val result: String?
        get() = if (inputted) texting.text else null

    private fun createTextArea() {
        texting = JTextArea(null, initial, 15, 26)
        texting.lineWrap = true
        texting.wrapStyleWord = true
        buttonOk.isEnabled = initial != null && !initial!!.isEmpty() && !requireChange
        val doc = texting.document
        texting.addCaretListener { e ->
            buttonOk.isEnabled = if (doc.length === 0) {
                canEmpty
            } else if (requireChange) {
                if (initial != null && !initial!!.isEmpty()) {
                    initial != texting.text
                } else {
                    true
                }
            } else {
                true
            }
        }
    }

    override fun createComponents(userPane: JPanel) {
        val buttonLoad = JButton(object : IAction("d.input.buttonLoad") {
            override fun actionPerformed(e: ActionEvent) {
                loadFile()
            }
        })

        buttonOk = JButton(object : IAction(BUTTON_OK.id) {
            override fun actionPerformed(e: ActionEvent) {
                inputted = true
                dispose()
            }
        })
        defaultButton = buttonOk

        createTextArea()

        val tipLabel = texting.labelled(tipText)
        tipLabel.border = BorderFactory.createEmptyBorder(0, 0, borderWidth, 0)

        userPane.add(tipLabel, BorderLayout.PAGE_START)
        userPane.add(JScrollPane(texting), BorderLayout.CENTER)

        controlsPane = createControlsPane(-1, buttonLoad, Box.createHorizontalGlue(), buttonOk, createCloseButton(BUTTON_CANCEL.id))
    }

    private fun loadFile() {
        val file = Dialogs.openFile(this, tr("d.input.openFile"), false)?.file ?: null
        if (file != null) {
            texting.text = file.readText()
        }
    }
}

internal class ErrorDialog : BaseDialog<Int> {
    lateinit var tipText: String
    lateinit var error: Throwable
    // if the text area is shown
    private var shown = false
    private lateinit var textView: JScrollPane

    constructor(owner: Frame, title: String, modal: Boolean) : super(owner, title, modal) {
        setDecorationIfNeed(JRootPane.ERROR_DIALOG)
    }

    constructor(owner: Dialog, title: String, modal: Boolean) : super(owner, title, modal) {
        setDecorationIfNeed(JRootPane.ERROR_DIALOG)
    }

    override val result: Int? = 0

    override fun createComponents(userPane: JPanel) {
        val tipLabel = mnemonicLabel(tipText)

        val originSize = Dimension(460, 138)
        val detailsSize = Dimension(480, 260)

        val reportButton = JButton(object : IAction("d.error.buttonReport") {
            override fun actionPerformed(e: ActionEvent?) {
                Dialogs.developing(this@ErrorDialog, tr("d.error.buttonReport" + tipTextSuffix))
            }
        })

        val detailsButton = JButton(object : IAction("d.error.buttonDetails") {
            override fun actionPerformed(e: ActionEvent) {
                if (shown) {
                    textView.isVisible = false
                    preferredSize = originSize
                } else {
                    textView = JScrollPane(createTextArea(error))
                    userPane.add(textView, BorderLayout.CENTER)
                    preferredSize = detailsSize
                }
                shown = !shown
                pack()
                setLocationRelativeTo(owner)
            }
        })

        controlsPane = createControlsPane(-1, reportButton, Box.createHorizontalGlue(), detailsButton, createCloseButton(BUTTON_CLOSE.id))

        userPane.add(tipLabel, BorderLayout.PAGE_START)

        preferredSize = originSize
    }

    private fun createTextArea(e: Throwable): JTextArea {
        val text = JTextArea()
        text.text = e.dump()
        text.isEditable = false
        text.caretPosition = 0
        return text
    }
}

class WaitingDialog : BaseDialog<Unit> {
    override val result: Unit? = null

    constructor(owner: Frame, title: String, modal: Boolean) : super(owner, title, modal) {
    }

    constructor(owner: Dialog, title: String, modal: Boolean) : super(owner, title, modal) {
    }

    private lateinit var progressBar: JProgressBar
    private lateinit var tipLabel: JLabel

    var tip = ""
    var waiting = ""
    var cancellable = false
    var progressable = false
    var cancelAction: (() -> Unit)? = null

    fun updateTip(str: String) {
        tipLabel.text = str
    }

    fun updateWaiting(str: String) {
        progressBar.string = str
    }

    var progress: Int get() = progressBar.value
        set(value) {
            progressBar.value = value
        }

    override fun createComponents(userPane: JPanel) {
        tipLabel = JLabel(tip)
        progressBar = JProgressBar()
        progressBar.minimum = 0
        progressBar.maximum = 100
        progressBar.value = 0
        progressBar.isStringPainted = true
        progressBar.string = waiting
        progressBar.isIndeterminate = progressable

        userPane.layout = GridLayout(2, 1)
        userPane.add(tipLabel)
        userPane.add(progressBar)

        val button = createCloseButton("d.waiting.buttonCancel")
        button.isEnabled = cancellable
        defaultButton = button
        controlsPane = createControlsPane(SwingConstants.RIGHT, button)
        preferredSize = Dimension(427, 118)
    }

    override fun cancelling() {
        if (cancellable) {
            cancelAction?.invoke()
            super.cancelling()
        }
    }
}

data class OpenResult(val files: Array<File>, val filter: FileFilter?) {
    val formats: Array<String>? = if (filter is FileNameExtensionFilter) filter.extensions else null

    val file: File? get() = if (files.isEmpty()) null else files[0]

    val format: String? get() = if (formats == null || formats.isEmpty()) null else formats[0]
}

object Dialogs {
    const val OPTION_OK = 0
    const val OPTION_CANCEL = 1
    const val OPTION_DISCARD = 2

    val fileChooser: JFileChooser = JFileChooser()

    fun saving(parent: Component?, title: String, message: Any): Int {
        return asking(parent, title, message, MessageStyle.Save)
    }

    fun asking(parent: Component?, title: String, message: Any, style: MessageStyle = MessageStyle.Question): Int {
        val option = notification(parent, title, message, style,
                -1, 2,
                Item("d.asking.discard"),
                Box.createHorizontalGlue(),
                Item("d.asking.ok"),
                Item("d.asking.cancel"))
        return when (option) {
            0 -> OPTION_DISCARD
            2 -> OPTION_OK
            else -> OPTION_CANCEL
        }
    }

    fun confirm(parent: Component?, title: String, showAsking: Boolean, message: Any): Pair<Boolean, Boolean> {
        return question(parent, title, showAsking, message)
    }

    fun text(parent: Component?, title: String, message: Any) {
        notification(parent, title, message, MessageStyle.Plain, SwingConstants.CENTER)
    }

    fun info(parent: Component?, title: String, message: Any) {
        notification(parent, title, message, MessageStyle.Information)
    }

    fun warn(parent: Component?, title: String, message: Any) {
        notification(parent, title, message, MessageStyle.Warning)
    }

    fun error(parent: Component?, title: String, message: Any) {
        notification(parent, title, message, MessageStyle.Alert)
    }

    fun selectDate(parent: Component?, title: String, tip: String, initial: Date?): Date? {
        val comp = JCalendar(initial)
        val option = notification(parent, title, arrayOf(tip, comp),
                MessageStyle.None,
                SwingConstants.RIGHT,
                0, BaseDialog.BUTTON_OK, BaseDialog.BUTTON_CANCEL)
        return if (option == 0) comp.date else null
    }

    fun selectLocale(parent: Component?, title: String, tip: String, initial: Locale?): Locale? {
        val comp = JLocaleChooser()
        comp.locale = initial
        val option = notification(parent, title, arrayOf(tip, comp),
                MessageStyle.None,
                SwingConstants.RIGHT,
                0, BaseDialog.BUTTON_OK, BaseDialog.BUTTON_CANCEL)
        return if (option == 0) comp.locale else null
    }

    @Suppress("unchecked_cast")
    fun <T> choosing(parent: Component?, title: String, tip: String, items: Array<T>, initial: T?, editable: Boolean = false): T? {
        val index = items.indexOf(initial)
        val comp = JComboBox(items)
        comp.isEditable = editable
        if (index != -1) {
            comp.selectedIndex = index
        } else if (initial != null) {
            comp.addItem(initial)
            comp.selectedIndex = comp.itemCount - 1
        } else {
            comp.selectedIndex = 0
        }
        val option = notification(parent, title, arrayOf(tip, comp),
                MessageStyle.None,
                SwingConstants.RIGHT,
                0, BaseDialog.BUTTON_OK, BaseDialog.BUTTON_CANCEL)
        return if (option == 0) comp.selectedItem as T else null
    }

    fun inputting(parent: Component?,
                  title: String,
                  tip: String,
                  initial: Any? = null,
                  requireChange: Boolean = false,
                  canEmpty: Boolean = true,
                  formatter: JFormattedTextField.AbstractFormatter? = null): Any? {
        val dialog = ICommonDialog.createDialog(parent, title, true, SimpleInputDialog::class.java)
        dialog.tipText = tip
        dialog.initial = initial
        dialog.requireChange = requireChange
        dialog.canEmpty = canEmpty
        dialog.formatter = formatter
        return dialog.showForResult(false)
    }

    fun inputText(parent: Component?,
                  title: String,
                  tip: String,
                  initial: String = "",
                  requireChange: Boolean = false,
                  canEmpty: Boolean = true): String? {
        return inputting(parent, title, tip, initial, requireChange, canEmpty, null) as? String
    }

    fun inputInteger(parent: Component?,
                     title: String,
                     tip: String,
                     initial: Long? = null,
                     requireChange: Boolean = false): Long? {
        return inputting(parent, title, tip, initial, requireChange, false,
                NumberFormatter(NumberFormat.getIntegerInstance())) as? Long
    }

    fun inputNumber(parent: Component?,
                    title: String,
                    tip: String,
                    initial: Number? = null,
                    requireChange: Boolean = false): Number? {
        return inputting(parent, title, tip, initial, requireChange, false,
                NumberFormatter(NumberFormat.getNumberInstance())) as? Number
    }

    fun longInput(parent: Component?,
                  title: String,
                  tip: String,
                  initial: String? = null,
                  requireChange: Boolean = false,
                  canEmpty: Boolean = true): String? {
        val dialog = ICommonDialog.createDialog(parent, title, true, LongInputDialog::class.java)
        dialog.tipText = tip
        dialog.initial = initial
        dialog.requireChange = requireChange
        dialog.canEmpty = canEmpty
        return dialog.showForResult(false)
    }

    fun trace(parent: Component?, title: String, tip: String, error: Throwable) {
        val dialog = ICommonDialog.createDialog(parent, title, true, ErrorDialog::class.java)
        dialog.tipText = tip
        dialog.error = error
        dialog.showForResult(true)
    }

    fun waiting(parent: Component, title: String, tip: String, waitingText: String): WaitingDialog {
        val dialog = ICommonDialog.createDialog(parent, title, true, WaitingDialog::class.java)
        dialog.tip = tip
        dialog.waiting = waitingText
        return dialog
    }

    fun initChooser(title: String,
                    mode: Int,
                    multiple: Boolean,
                    initFile: File?, initDir: File?,
                    acceptAll: Boolean,
                    filters: Array<FileFilter>?, initFilter: FileFilter?) {
        fileChooser.dialogTitle = title
        fileChooser.fileSelectionMode = mode
        fileChooser.isMultiSelectionEnabled = multiple
        if (initDir != null) {
            fileChooser.currentDirectory = initDir
        }
        if (initFile != null) {
            fileChooser.selectedFile = initFile
        }
        // filters
        fileChooser.resetChoosableFileFilters()
        fileChooser.isAcceptAllFileFilterUsed = acceptAll
        if (filters != null) {
            for (filter in filters) {
                fileChooser.addChoosableFileFilter(filter)
            }
        }
        if (initFilter != null) {
            fileChooser.fileFilter = initFilter
        }
    }

    fun setApproveButtonName(name: String, tip: String) {
        val parts = Ixin.mnemonicOf(name)
        fileChooser.approveButtonText = parts.name
        fileChooser.approveButtonMnemonic = parts.mnemonic
        fileChooser.approveButtonToolTipText = tip
    }

    fun setApproveButtonName(id: String) {
        setApproveButtonName(tr(id), tr(id + IAction.Companion.tipTextSuffix))
    }

    private fun selectedFile(): File {
        var file = fileChooser.selectedFile

        if (PathUtils.extName(file.path).isEmpty()) {
            // append extension by choose extension filter
            val filter = fileChooser.fileFilter
            if (filter is FileNameExtensionFilter) {
                file = File(file.path + '.' + filter.extensions[0])
            }
        }

        return file
    }

    fun setDirectory(dir: File?) {
        if (dir != null) {
            fileChooser.currentDirectory = dir
        }
    }

    fun openFile(parent: Component?,
                 title: String,
                 multiple: Boolean,
                 initFile: File? = null, initDir: File? = null,
                 acceptAll: Boolean = true,
                 filters: Array<FileFilter>? = null, initFilter: FileFilter? = null): OpenResult? {
        initChooser(title, JFileChooser.FILES_ONLY, multiple, initFile, initDir, acceptAll, filters, initFilter)
        setApproveButtonName("d.openFile.approveButton")
        if (fileChooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null
        }
        val files = if (multiple) {
            fileChooser.selectedFiles
        } else {
            arrayOf(fileChooser.selectedFile)
        }
        return OpenResult(files, fileChooser.fileFilter)
    }

    fun saveFile(parent: Component?,
                 title: String,
                 initFile: File? = null, initDir: File? = null,
                 askOverwrite: Boolean = true,
                 acceptAll: Boolean = true,
                 filters: Array<FileFilter>? = null, initFilter: FileFilter? = null): OpenResult? {
        initChooser(title, JFileChooser.FILES_ONLY, false, initFile, initDir, acceptAll, filters, initFilter)
        setApproveButtonName("d.saveFile.approveButton")
        while (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            val file = selectedFile()

            if (file.exists() && askOverwrite) {
                if (!confirm(parent, title, false, tr("d.saveFile.askOverwrite", file.path)).first) {
                    continue
                }
            }

            return OpenResult(arrayOf(file), fileChooser.fileFilter)
        }

        // cancelled
        return null
    }

    val extensionDescriptions = HashMap<String, String>()

    // [desc] (*.xxx *.yyy *.zzz)
    private fun descriptionOfExtensions(extensions: Array<String>): String {
        val key = extensions[0]
        val v = extensionDescriptions.getOrPut(key) {
            var name: String? = null
            try {
                name = tr("common.extension." + key.toLowerCase())
            } catch (e: MissingResourceException) {
                App.error("no such extension key: $key")
            }

            if (name.isNullOrEmpty()) {
                name = key.toUpperCase() + tr("common.extension.suffix")
            }
            name!!
        }
        return "$v (*.${extensions.joinToString(" *.")})"
    }

    @Suppress("unchecked_cast")
    fun extensionFilterFor(format: Any): FileNameExtensionFilter {
        val extensions: Array<String> = if (format is Array<*>) {
            format as Array<String>
        } else {
            arrayOf(format.toString())
        }
        return FileNameExtensionFilter(descriptionOfExtensions(extensions), *extensions)
    }

    @Suppress("unchecked_cast")
    fun extensionFiltersFor(formats: Array<Any>, initFormat: Any?): Pair<Array<FileFilter>, FileFilter?> {
        val filters = ArrayList<FileFilter>(formats.size)
        var initFilter: FileFilter? = null
        var isEqual = false
        for (format in formats) {
            val extensions: Array<String>
            if (format is String) {
                extensions = arrayOf(format)
                if (initFormat != null) {
                    isEqual = initFormat == format
                }
            } else if (format is Array<*>) {
                extensions = format as Array<String>
                if (initFormat != null) {
                    if (initFormat is String) {
                        isEqual = initFormat in extensions
                    } else if (initFormat is Array<*>) {
                        isEqual = Arrays.equals(extensions, initFormat)
                    }
                }
            } else {
                continue
            }
            val filter = extensionFilterFor(extensions)
            filters.add(filter)
            if (isEqual) {
                initFilter = filter
            }
        }
        return filters.toTypedArray() to initFilter
    }

    fun selectFile(parent: Component?,
                   title: String,
                   initFile: File? = null,
                   formats: Array<Any>? = null,
                   initFormat: Any? = null,
                   acceptAll: Boolean = true, openMode: Boolean = true, multiple: Boolean = false): OpenResult? {
        var filters: Array<FileFilter>? = null
        var initFilter: FileFilter? = null
        if (formats != null) {
            val objects = extensionFiltersFor(formats, initFormat)
            filters = objects.first
            initFilter = objects.second
        }
        return if (openMode) {
            openFile(parent, title, multiple, initFile, null, acceptAll, filters, initFilter)
        } else {
            saveFile(parent, title, initFile, null, true, acceptAll, filters, initFilter)
        }
    }

    fun selectOpenFile(parent: Component?,
                       title: String,
                       initFile: File? = null,
                       formats: Array<Any>? = null,
                       initFormat: Any? = null,
                       acceptAll: Boolean = true, multiple: Boolean = false): OpenResult? {
        return selectFile(parent, title, initFile, formats, initFormat, acceptAll, true, multiple)
    }

    fun selectSaveFile(parent: Component?,
                       title: String,
                       initFile: File? = null,
                       formats: Array<Any>? = null,
                       initFormat: Any? = null, acceptAll: Boolean = true): OpenResult? {
        return selectFile(parent, title, initFile, formats, initFormat, acceptAll, false, false)
    }

    fun selectOpenImage(parent: Component?, title: String): OpenResult? {
        val jpg = arrayOf("jpg", "jpeg")
        return selectOpenFile(parent, title, null, arrayOf<Any>(jpg, "png", "gif", "bmp"), jpg, true, false)
    }

    fun selectSaveImage(parent: Component?, title: String): OpenResult? {
        val jpg = arrayOf("jpg", "jpeg")
        return selectSaveFile(parent, title, null, arrayOf<Any>(jpg, "png", "gif", "bmp"), jpg, false)
    }

    fun editSettings(parent: Frame) {
//        EditAppSettings(parent).setVisible(true)
    }

    fun showAbout(parent: Frame) {
//        AboutImabw(parent).setVisible(true)
    }

    fun editAttributes(parent: Component?, chapter: Chapter) {
//        val dialog = createDialog(parent,
//                app.getText("attributes.title", book.getTitle()),
//                true, ChapterAttributes::class.java)
//        dialog.setBook(book)
//        dialog.makeShow(true)
    }

    fun editExtensions(parent: Component?, book: Book) {
//        val dialog = createDialog(parent,
//                app.getText("extensions.title", book.getTitle()),
//                true, EditExtensions::class.java)
//        dialog.setBook(book)
//        dialog.makeShow(true)
    }

    fun bookDetails(parent: Component?, book: Book, file: File?) {

    }

    fun developing(parent: Component, title: String? = null) {
        info(parent, title ?: tr("app.name"), tr("d.feature.developing"))
    }

    fun browse(uri: String?) {
        if (uri.isNullOrEmpty()) {
            return
        }
        try {
            Desktop.getDesktop().browse(URI(uri))
        } catch (ex: Exception) {
            error(null, tr("app.name"), ex.message ?: "")
        }

    }

    fun notification(parent: Component?,
                     title: String,
                     message: Any,
                     style: MessageStyle,
                     alignment: Int = SwingConstants.RIGHT,
                     selection: Int = 0,
                     vararg options: Any): Int? {
        val dialog = MessageDialog.create(parent, title, message, style)
        if (options.isNotEmpty()) {
            dialog.setOptions(alignment, selection, *options)
        } else {
            dialog.addCloseButton(alignment)
        }
        return dialog.showForOption()
    }

    fun question(parent: Component?, title: String, showAsking: Boolean, message: Any): Pair<Boolean, Boolean> {
        val dialog = MessageDialog.create(parent, title, message, MessageStyle.Question)
        if (showAsking) {
            val button = IgnoredAction("d.common.checkNotAskAgain").asButton(Style.CHECK)
            button.isOpaque = false
            dialog.setOptions(-1, 2, button, Box.createHorizontalGlue(), BaseDialog.BUTTON_OK, BaseDialog.BUTTON_CANCEL)
            return (dialog.showForOption() == 2) to button.isSelected
        } else {
            dialog.setRightAlignedOptions(0, BaseDialog.BUTTON_OK, BaseDialog.BUTTON_CANCEL)
            return (dialog.showForOption() == 0) to false
        }
    }
}
