package pw.phylame.imabw.activity

import android.app.Activity
import android.content.Context
import android.content.Intent

fun Context.getQuantityString(id: Int, quantity: Int): String = resources.getQuantityString(id, quantity)

fun Context.getQuantityString(id: Int, quantity: Int, vararg args: Any): String = if (args.isNotEmpty()) {
    resources.getQuantityString(id, quantity, *args)
} else {
    resources.getQuantityString(id, quantity)
}

fun Activity.startActivity(target: Class<out Activity>, init: (Intent.() -> Unit)? = null) {
    val intent = Intent(this, target)
    init?.invoke(intent)
    startActivity(intent)
}

fun Activity.startActivityForResult(target: Class<out Activity>, requestCode: Int, init: (Intent.() -> Unit)? = null) {
    val intent = Intent(this, target)
    init?.invoke(intent)
    startActivityForResult(intent, requestCode)
}
