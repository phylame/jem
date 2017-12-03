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

import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.SplitPane
import javafx.scene.control.TextArea
import javafx.scene.control.TitledPane
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.text.Text
import jem.*
import mala.App


class AttributePane(chapter: Chapter) : SplitPane() {
    private lateinit var coverPane: Node

    private lateinit var coverView: ImageView

    private lateinit var introEditor: TextArea

    private val variantPane = object : VariantPane(chapter.attributes) {
        override fun availableKeys() = Attributes.names

        override fun ignoredKeys() = listOf(TITLE, COVER, INTRO)

        override fun getItemType(key: String) = Attributes.getType(key)

        override fun getDefaultValue(key: String) = Attributes.getDefault(key) ?: ""

        override fun getItemName(key: String) = Attributes.getName(key) ?: key.capitalize()

        override fun newDialogTitle() = App.tr("d.newAttribute.title")
    }

    var isModified = false

    init {
        createCoverPane()
        createIntroPane()
//        items += TitledPane("Cover", coverPane).apply {
//            SplitPane.setResizableWithParent(this, false)
//            isCollapsible = false
//        }
        items += coverPane
        items += SplitPane().apply {
            orientation = Orientation.VERTICAL
            items += TitledPane("Attributes", variantPane).apply {
                isCollapsible = false
            }
            items += TitledPane("Intro", introEditor).apply {
                isCollapsible = false
            }
        }
    }

    fun syncVariants() {
        variantPane.syncVariants()
    }

    private fun createCoverPane() {
        coverView = ImageView()
        val altText = Text("No Cover")
        val stackPane = StackPane(coverView, altText).apply {
            AnchorPane.setTopAnchor(this, 0.0)
            AnchorPane.setRightAnchor(this, 0.0)
            AnchorPane.setLeftAnchor(this, 0.0)
            AnchorPane.setBottomAnchor(this, 0.0)
        }
        coverPane = BorderPane(TitledPane("Cover", stackPane).apply {
            this.border = Border.EMPTY
        }).apply {
            bottom = HBox().apply {
                alignment = Pos.CENTER
                children.addAll(Button("Open"), Button("Save"), Button("Remove"))
            }
        }
    }

    private fun createIntroPane() {
        introEditor = TextArea().apply {
            isWrapText = true
        }
    }
}
