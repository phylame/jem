package test

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.ToolBar
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import mala.ixin.Action
import mala.ixin.ButtonType

fun main(args: Array<String>) {
    Application.launch(App::class.java, *args)
}

class App : Application() {
    override fun start(stage: Stage) {
        val root = BorderPane()

        val action = Action("open-file")
        action.text = "_Open"
        action.toast = "Open File"

        root.top = VBox().apply {
            val menuBar = MenuBar()
            menuBar.menus += Menu("_File").apply {
                items += action.toMenuItem()
                items += action.toMenuItem(ButtonType.RADIO)
                items += action.toMenuItem(ButtonType.CHECK)
            }
            menuBar.menus += Menu("_Edit").apply {
                items += action.toMenuItem()
            }
            children += menuBar

            val toolBar = ToolBar()
            toolBar.items += action.toButton()
            toolBar.items += action.toButton(ButtonType.CHECK)
            toolBar.items += action.toButton(ButtonType.RADIO)
            toolBar.items += action.toButton(ButtonType.TOGGLE)
            toolBar.items += action.toButton(ButtonType.LINK)
            children += toolBar
        }

        stage.scene = Scene(root, 400.0, 350.0)
        stage.show()
    }
}
