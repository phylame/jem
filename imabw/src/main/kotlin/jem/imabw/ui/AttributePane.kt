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
import jclp.io.Flob
import jclp.io.subMime
import jem.*
import jem.imabw.Imabw
import jem.imabw.JemSettings
import jem.imabw.LoadTextTask
import jem.imabw.saveFlob
import mala.App
import mala.App.tr
import mala.ixin.clearAnchorConstraints
import mala.ixin.plusAssign
import java.net.URL
import java.util.*

class AttributePane(val chapter: Chapter) : AnchorPane() {
    private val variantPane = object : VariantPane(chapter.attributes, "attributes") {
        override fun availableKeys() = Attributes.names

        override fun ignoredKeys() = listOf(TITLE, COVER, INTRO)

        override fun getItemType(key: String) = Attributes.getType(key)

        override fun getDefaultValue(key: String) = Attributes.getDefault(key) ?: ""

        override fun getItemTitle(key: String) = Attributes.getTitle(key) ?: key.capitalize()

        override fun getAvailableValues(key: String): List<String> {
            return when (key) {
                STATE -> JemSettings.states.split(';').dropWhile { it.isEmpty() }
                GENRE -> JemSettings.genres.split(';').dropWhile { it.isEmpty() }
                else -> emptyList()
            }
        }

        override fun dialogNewTitle() = App.tr("d.newAttribute.title")
    }

    private var isChanged: Boolean = false

    private val viewController: AttributeViewController

    val isModified get() = isChanged || variantPane.isModified

    init {
        val loader = FXMLLoader(App.assets.resourceFor("ui/AttributePane.fxml"))
        val root = loader.load<SplitPane>().apply { clearAnchorConstraints() }
        viewController = loader.getController()
        initView()
        loadCover(chapter.cover)
        loadIntro()
        this += root
    }

    fun syncVariants() {
        variantPane.syncVariants()
    }

    fun storeState() {
        variantPane.storeState()
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
                    val type = chapter.cover?.mimeType?.let { ", ${subMime(it).toUpperCase()}" } ?: ""
                    coverInfo.text = "${image.width.toInt()}x${image.height.toInt()}$type"
                }
            }
            openButton.setOnAction {
                selectOpenImage("", scene.window)
            }
            saveButton.setOnAction {
                val cover = chapter.cover!!
                val title = tr("d.saveCover.title")
                selectSaveFile(title, "cover.${subMime(cover.mimeType)}", scene.window)?.let {
                    saveFlob(title, cover, it, scene.window)
                }
            }
            removeButton.setOnAction {
                loadCover(null)
                isChanged = true
            }
            attributeTitle.content = variantPane
        }
    }

    private fun loadCover(flob: Flob?) {
        with(viewController) {
            coverView.image = flob?.let {
                it.openStream().use { Image(it) }
            }
        }
    }

    private fun loadIntro() {
        chapter.intro?.let { text ->
            with(LoadTextTask(text)) {
                setOnRunning {
                    updateProgress(App.tr("jem.loadText.hint", chapter.title))
                }
                setOnSucceeded {
                    viewController.introEditor.text = value
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
    lateinit var openButton: Button

    @FXML
    lateinit var saveButton: Button

    @FXML
    lateinit var removeButton: Button

    @FXML
    lateinit var attributeTitle: TitledPane

    @FXML
    lateinit var introTitle: TitledPane

    @FXML
    lateinit var introEditor: TextArea

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        coverTitle.text = tr("d.editAttribute.cover.title")
        coverAlt.text = tr("d.editAttribute.cover.alt")
        openButton.text = tr("ui.button.open")
        saveButton.text = tr("ui.button.save")
        removeButton.text = tr("ui.button.remove")
        attributeTitle.text = tr("d.editAttribute.attributes.title")
        introTitle.text = tr("d.editAttribute.intro.title")
        introEditor.promptText = tr("d.editAttribute.intro.hint")
    }
}
