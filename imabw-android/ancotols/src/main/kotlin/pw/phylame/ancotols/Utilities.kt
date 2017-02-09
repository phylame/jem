package pw.phylame.ancotols

import android.content.res.Resources
import android.util.SparseArray
import android.util.TypedValue

val IgnoredAction: () -> Unit = {}

private val _ReuseValue = TypedValue()

fun dimenFor(resources: Resources, id: Int): Float {
    val value = _ReuseValue
    resources.getValue(id, value, true)
    return TypedValue.complexToFloat(value.data)
}

operator fun <T> SparseArray<T>.set(key: Int, value: T) = put(key, value)

fun <T : CharSequence> T?.or(supplier: () -> T): T = if (isNullOrEmpty()) supplier() else this!!

fun <T> MutableCollection<T>.setAll(elements: Collection<T>) {
    clear()
    addAll(elements)
}

fun <T> MutableCollection<T>.setAll(elements: Iterable<T>) {
    clear()
    addAll(elements)
}

fun <T> MutableCollection<T>.setAll(elements: Sequence<T>) {
    clear()
    addAll(elements)
}

class TimedAction(val limit: Long) {
    private var lastTime = 0L

    val isEnable: Boolean get() {
        val now = System.currentTimeMillis()
        if (now - lastTime < limit) {
            lastTime = 0
            return true
        } else {
            lastTime = now
            return false
        }
    }
}
