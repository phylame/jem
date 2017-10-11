package mala.ixin

import javafx.scene.image.Image
import javafx.scene.image.ImageView
import jclp.io.extName
import jclp.text.or
import mala.AssetManager

fun AssetManager.graphicFor(name: String) = imageFor(name)?.let(::ImageView)

fun AssetManager.imageFor(name: String) = resourceFor(IxIn.gfxPath(name))?.openStream()?.use(::Image)

fun AssetManager.designerFor(name: String) = resourceFor(name)?.openStream()?.use(::JSONDesigner)

object IxIn {
    var iconSet = System.getProperty("ixin.icons") or "default"

    var largeIconSuffix = System.getProperty("ixin.largeIconSuffix") or "@x"

    var selectedIconSuffix = System.getProperty("ixin.selectedIconSuffix") or "-selected"

    var isMnemonicEnable = "mac" !in System.getProperty("os.name")

    fun gfxPath(name: String): String {
        val dir = "gfx/${if (iconSet.isNotEmpty()) iconSet + '/' else ""}"
        return "$dir${if (extName(name).isNotEmpty()) name else name + ".png"}"
    }
}
