package pw.phylame.ancotols

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.media.ThumbnailUtils
import android.os.Build
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.ListView
import java.io.InputStream

inline operator fun <reified T : View> View.get(id: Int): T = findViewById(id) as T

inline fun <reified T : View> View.lazyView(id: Int): Lazy<T> = lazy { findViewById(id) as T }

val MenuItem.position: Int get() = (this.menuInfo as AdapterView.AdapterContextMenuInfo).position

fun coloredView(context: Context, color: Int, width: Int, height: Int): View {
    val view = View(context)
    view.layoutParams = LinearLayout.LayoutParams(width, height)
    view.setBackgroundColor(color)
    return view
}

fun <T : Any> reusedView(convertView: View?, context: Context, viewId: Int, init: (View) -> T): Pair<View, T> {
    if (convertView == null) {
        val view = LayoutInflater.from(context).inflate(viewId, null)
        val holder = init(view)
        view.tag = holder
        return view to holder
    } else {
        @Suppress("unchecked_cast")
        return convertView to convertView.tag as T
    }
}

fun showProgress(context: Context, progressBar: View, shown: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
        val shortAnimTime = context.resources.getInteger(android.R.integer.config_shortAnimTime)
        progressBar.visibility = if (shown) View.VISIBLE else View.GONE
        progressBar.animate()
                .setDuration(shortAnimTime.toLong())
                .alpha(if (shown) 1F else 0F)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        progressBar.visibility = if (shown) View.VISIBLE else View.GONE
                    }
                })
    } else {
        progressBar.visibility = if (shown) View.VISIBLE else View.GONE
    }
}

var ListView.topPosition: Pair<Int, Int>
    get() {
        var index = firstVisiblePosition
        var view: View? = getChildAt(0)
        var top = if (view == null) 0 else view.top
        if (top < 0 && getChildAt(1) != null) {
            index++
            view = getChildAt(1)
            top = view!!.top
        }
        return index to top
    }
    set(value) {
        if (value.second < 0) {
            setSelection(value.first)
        } else {
            setSelectionFromTop(value.first, value.second)
        }
    }

fun calculateSimpleSize(inWidth: Int, inHeight: Int, outWidth: Int, outHeight: Int): Int {
    return Math.min(inWidth / outHeight.toDouble(), inHeight / outHeight.toDouble()).toInt()
}

fun loadBitmap(input: InputStream, width: Int, height: Int) {

}
