package mala.ixin

import javafx.scene.image.Image
import javafx.scene.image.ImageView
import jclp.io.extName
import jclp.setting.delegate
import jclp.text.or
import mala.App
import mala.AssetManager
import mala.MalaSettings

fun AssetManager.graphicFor(name: String) = imageFor(name)?.let(::ImageView)

fun AssetManager.imageFor(name: String) = resourceFor(IxIn.gfxPath(name))?.openStream()?.use(::Image)

fun AssetManager.designerFor(name: String) = resourceFor(name)?.openStream()?.use(::JSONDesigner)

object IxIn {
    val delegate = App.delegate as IDelegate

    val menuMap get() = delegate.fxApp.menuMap

    val actionMap get() = delegate.fxApp.actionMap

    fun newAction(id: String) = actionMap.getOrCreate(id, App, App.assets)

    var iconSet = System.getProperty("ixin.icons") or "default"

    var largeIconSuffix = System.getProperty("ixin.largeIconSuffix") or "@x"

    var selectedIconSuffix = System.getProperty("ixin.selectedIconSuffix") or "-selected"

    var isMnemonicEnable = "mac" !in System.getProperty("os.name")

    fun gfxPath(name: String) = "gfx/$iconSet/${if (extName(name).isNotEmpty()) name else name + ".png"}"
}

open class IxInSettings(name: String = "config/ui.ini") : MalaSettings(name) {
    var stageX by delegate(-1.0, "form.location.x")
    var stageY by delegate(-1.0, "form.location.y")
    var stageWidth by delegate(-1.0, "form.location.width")
    var stageHeight by delegate(-1.0, "form.location.height")

    var stageAlwaysOnTop by delegate(false, "form.alwaysOnTop")
    var stageResizable by delegate(true, "form.size.resizable")

    var toolBarVisible by delegate(true, "form.toolBar.visible")
    var statusBarVisible by delegate(true, "form.statusBar.visible")
}
