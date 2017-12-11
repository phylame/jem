/*
 * Copyright 2015-2017 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
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

package jem.imabw.ui

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import jclp.io.flobOf
import jclp.io.subMime
import jclp.setting.getDouble
import jclp.text.textOf
import jem.*
import jem.imabw.*
import mala.App
import mala.App.tr
import mala.ixin.clearAnchorConstraints
import java.net.URL
import java.util.*

class AttributePane(val chapter: Chapter) : AnchorPane() {
    private val variantPane = object : VariantPane(chapter.attributes, "attributes") {
        override fun availableKeys() = Attributes.names

        override fun ignoredKeys() = listOf(TITLE, COVER, INTRO)

        override fun getItemType(key: String) = Attributes.getType(key)

        override fun getDefaultValue(key: String) =
                JemSettings.getValue(key) ?: Attributes.getDefault(key) ?: ""

        override fun isMultiValues(key: String): Boolean = when (key) {
            in JemSettings.multipleAttrs.split(Attributes.VALUE_SEPARATOR) -> true
            else -> false
        }

        override fun getItemTitle(key: String) = Attributes.getTitle(key) ?: key.capitalize()

        override fun getAvailableValues(key: String): List<String> {
            return when (key) {
                STATE -> JemSettings.states.split(';').filter { it.isNotEmpty() }
                GENRE -> JemSettings.genres.split(';').filter { it.isNotEmpty() }
                else -> emptyList()
            }
        }

        override fun dialogNewTitle() = App.tr("d.newAttribute.title")
    }

    private var cover = chapter.cover
        set(value) {
            isChanged = true
            field = value
        }

    private var isChanged: Boolean = false

    private val viewController: AttributeViewController

    val isModified: Boolean get() = isChanged || variantPane.isModified

    init {
        val loader = FXMLLoader(App.assets.resourceFor("ui/AttributePane.fxml"))
        val root = loader.load<SplitPane>().apply { clearAnchorConstraints() }
        viewController = loader.getController()
        initView()
        loadCover()
        loadIntro()
        children += root
    }

    fun syncVariants() {
        variantPane.syncVariants()
        if (cover == null) {
            chapter.attributes.remove(COVER)
        } else {
            chapter.cover = cover
        }
        viewController.introEditor.text.let {
            if (it.isEmpty()) {
                chapter.attributes.remove(INTRO)
            } else {
                chapter.intro = textOf(it)
            }
        }
    }

    fun storeState() {
        variantPane.storeState()
        viewController.storeState()
    }

    private fun initView() {
        with(viewController) {
            coverView.imageProperty().addListener { _, _, image ->
                if (image == null) {
                    saveButton.isDisable = true
                    removeButton.isDisable = true
                    coverInfo.text = ""
                } else {
                    saveButton.isDisable = false
                    removeButton.isDisable = false
                    val type = cover?.mimeType?.let { ", ${subMime(it).toUpperCase()}" } ?: ""
                    coverInfo.text = "${image.width.toInt()}x${image.height.toInt()}$type"
                }
            }
            openButton.setOnAction {
                selectOpenImage(tr("d.selectCover.title"), scene.window)?.let { file ->
                    file.inputStream().use { Image(it) }.let { img ->
                        if (img.isError) {
                            debug(tr("d.selectCover.title"), tr("d.openCover.error", file), img.exception, scene.window)
                        } else {
                            cover = flobOf(file)
                            coverView.image = img
                        }
                    }
                }
            }
            saveButton.setOnAction {
                val cover = cover!!
                val title = tr("d.saveCover.title")
                selectSaveFile(title, "cover.${subMime(cover.mimeType)}", scene.window)?.let {
                    saveFlob(title, cover, it, scene.window)
                }
            }
            removeButton.setOnAction {
                cover = null
                loadCover()
            }
            attributeTitle.content = variantPane
            introEditor.textProperty().addListener { _ -> isChanged = true }
        }
    }

    private fun loadCover() {
        with(viewController) {
            coverView.image = cover?.let {
                it.openStream().use { Image(it) }
            }
        }
    }

    private fun loadIntro() {
        chapter.intro?.let { text ->
            with(LoadTextTask(text, App.tr("jem.loadText.hint", chapter.title))) {
                setOnSucceeded {
                    viewController.introEditor.text = value
                    isChanged = false
                    hideProgress()
                }
                Imabw.submit(this)
            }
        }
    }
}

internal class AttributeViewController : Initializable {
    @FXML
    lateinit var root: SplitPane

    @FXML
    lateinit var coverTitle: TitledPane

    @FXML
    lateinit var coverAlt: Label

    @FXML
    lateinit var coverView: ImageView

    @FXML
    lateinit var coverInfo: Label

    @FXML
    lateinit var makeButton: Button

    @FXML
    lateinit var openButton: Button

    @FXML
    lateinit var saveButton: Button

    @FXML
    lateinit var removeButton: Button

    @FXML
    lateinit var vSplit: SplitPane

    @FXML
    lateinit var attributeTitle: TitledPane

    @FXML
    lateinit var introTitle: TitledPane

    @FXML
    lateinit var introEditor: TextArea

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        coverTitle.text = tr("d.editAttribute.cover.title")
        coverAlt.text = tr("d.editAttribute.cover.alt")
        coverInfo.text = ""
        makeButton.text = tr("ui.button.make")
        makeButton.isDisable = true
        openButton.text = tr("ui.button.open")
        saveButton.text = tr("ui.button.save")
        removeButton.text = tr("ui.button.remove")
        attributeTitle.text = tr("d.editAttribute.attributes.title")
        introTitle.text = tr("d.editAttribute.intro.title")
        introEditor.promptText = tr("d.editAttribute.intro.hint")

        coverView.imageProperty().addListener { _, _, image ->
            coverView.fitWidth = minOf(UISettings.minCoverWidth, image.width)
        }

        val settings = UISettings
        settings.getDouble("dialog.attributes.split.0.position")?.let {
            root.setDividerPosition(0, it)
        }
        settings.getDouble("dialog.attributes.split.1.position")?.let {
            vSplit.setDividerPosition(0, it)
        }
    }

    fun storeState() {
        val settings = UISettings
        settings["dialog.attributes.split.0.position"] = root.dividerPositions.first()
        settings["dialog.attributes.split.1.position"] = vSplit.dividerPositions.first()
    }
}
