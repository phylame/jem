package mala.ixin

import javafx.scene.image.Image
import jclp.io.extName
import jclp.io.openResource
import jclp.text.or
import mala.AssetManager

fun AssetManager.gfxPath(name: String): String {
    val dir = "gfx/${if (IxIn.iconSet.isNotEmpty()) IxIn.iconSet + '/' else ""}"
    return "$dir${if (extName(name).isNotEmpty()) name else name + ".png"}"
}

fun AssetManager.imageFor(name: String, loader: ClassLoader? = null): Image? {
    return openResource(pathOf(gfxPath(name)), loader)?.use(::Image)
}

object IxIn {
    var iconSet = System.getProperty("ixin.icons") or { "default" }

    var isMnemonicEnable = "mac" !in System.getProperty("os.name")
}
