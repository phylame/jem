/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

package mala.ixin

import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.Menu
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.Screen
import javafx.stage.Stage
import mala.App
import mala.AppDelegate
import mala.Plugin

interface IPlugin : Plugin {
    fun ready() {}
}

abstract class IDelegate : AppDelegate, CommandHandler {
    val commandProxy = CommandDispatcher()

    lateinit var fxApp: IApplication
        internal set

    override fun handle(command: String, source: Any): Boolean {
        if (!commandProxy.handle(command, source)) {
            App.error("No handler for command: '$command'")
            return false
        }
        return true
    }

    open fun onReady() {}
}

typealias MenuMap = MutableMap<String, Menu>
typealias ActionMap = MutableMap<String, Action>

abstract class IApplication : Application() {
    lateinit var stage: Stage

    lateinit var appPane: AppPane

    val menuMap = hashMapOf<String, Menu>()

    val actionMap = hashMapOf<String, Action>()

    var statusText
        get() = appPane.statusBar?.statusLabel?.text
        set(value) {
            appPane.statusBar?.statusLabel?.text = value
        }

    override fun start(primaryStage: Stage) {
        IxIn.delegate.fxApp = this
        stage = primaryStage

        primaryStage.icons += App.assets.imageFor("icon")
        primaryStage.setOnCloseRequest {
            IxIn.delegate.handle("exit", stage)
            it.consume()
        }

        appPane = AppPane()
        primaryStage.scene = Scene(appPane).apply {
            setup(this, appPane)
        }

        App.plugins.with<IPlugin> { ready() }
        IxIn.delegate.onReady()

        primaryStage.show()
    }

    open fun setup(scene: Scene, appPane: AppPane) {}

    open fun restoreState(settings: IxInSettings) {
        val stage = stage

        val bounds = Screen.getPrimary().visualBounds
        stage.width = settings.stageWidth.takeUnless { it < 0 } ?: bounds.width * 0.6
        stage.height = settings.stageHeight.takeUnless { it < 0 } ?: bounds.height * 0.6
        if (settings.stageX >= 0 && settings.stageY >= 0) {
            stage.x = settings.stageX
            stage.y = settings.stageY
        }

        stage.isResizable = settings.stageResizable
        stage.isAlwaysOnTop = settings.stageAlwaysOnTop

        appPane.toolBar?.isVisible = settings.toolBarVisible
        appPane.statusBar?.isVisible = settings.statusBarVisible
    }

    open fun saveState(settings: IxInSettings) {
        settings.stageX = stage.x
        settings.stageY = stage.y
        settings.stageWidth = stage.width
        settings.stageHeight = stage.height

        settings.stageResizable = stage.isResizable
        settings.stageAlwaysOnTop = stage.isAlwaysOnTop

        settings.toolBarVisible = appPane.toolBar!!.isVisible
        settings.statusBarVisible = appPane.statusBar!!.isVisible
    }

    private val progressLabel = Label().apply { styleClass += "app-progress-label" }

    fun showProgress() {
        appPane.statusBar?.left = HBox(4.0).apply {
            alignment = Pos.CENTER
            BorderPane.setAlignment(this, Pos.CENTER)
            children += ProgressIndicator().also { it.styleClass += "app-progress-indicator" }
            children += progressLabel
        }
    }

    fun updateProgress(text: String) {
        progressLabel.text = text
    }

    fun hideProgress() {
        appPane.statusBar?.left = appPane.statusBar?.statusLabel
    }
}
