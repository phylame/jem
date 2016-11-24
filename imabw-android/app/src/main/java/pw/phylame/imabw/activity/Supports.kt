package pw.phylame.imabw.activity

import android.content.Context

fun Context.getQuantityString(id: Int, quantity: Int): String = resources.getQuantityString(id, quantity)

fun Context.getQuantityString(id: Int, quantity: Int, vararg args: Any): String = if (args.isNotEmpty()) {
    resources.getQuantityString(id, quantity, *args)
} else {
    resources.getQuantityString(id, quantity)
}