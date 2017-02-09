package pw.phylame.ancotols

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.annotation.*
import android.support.v4.content.ContextCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.util.SparseArray
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Toast
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*

/**
 * Indicates function performing a menu selecting action.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MenuAction(val id: Int)

/**
 * Indicates function performing action when result activity finished.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ResultAction(val code: Int)

abstract class ManagedActivity : AppCompatActivity() {
    companion object {
        private val activities = LinkedList<Activity>()

        fun finishAll() {
            activities.forEach(Activity::finish)
        }
    }

    /**
     * Tag for logger.
     */
    protected val TAG: String by lazy {
        javaClass.simpleName
    }

    protected var isActionDriven = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activities.add(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return performMenuAction(item) || super.onOptionsItemSelected(item)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return performMenuAction(item) || super.onContextItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!performResultAction(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun performMenuAction(item: MenuItem): Boolean {
        if (!isActionDriven) {
            return false
        }
        val method = actionHolder.menuActions.get(item.itemId)
        if (method != null) {
            if (method.parameterTypes.isEmpty()) {
                method.invoke(this)
            } else {
                method.invoke(this, item)
            }
            return true
        }
        return false
    }

    private fun performResultAction(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (!isActionDriven) {
            return false
        }
        val method = actionHolder.resultActions.get(requestCode)
        if (method != null) {
            if (method.parameterTypes.isEmpty()) {
                method.invoke(this)
            } else {
                method.invoke(this, resultCode, data)
            }
            return true
        }
        return false
    }

    private val actionHolder by lazy { ActionHolder(this) }

    private class ActionHolder(activity: Activity) {
        val menuActions = SparseArray<Method>()

        val resultActions = SparseArray<Method>()

        init {
            activity.javaClass.methods
                    .filter { !Modifier.isStatic(it.modifiers) && !Modifier.isAbstract(it.modifiers) }
                    .forEach {
                        for (annotation in it.annotations) {
                            when (annotation) {
                                is MenuAction -> menuActions[annotation.id] = it
                                is ResultAction -> resultActions[annotation.code] = it
                            }
                        }
                    }
        }
    }
}

fun Activity.lazyDimen(@DimenRes id: Int): Lazy<Float> = lazy { dimenFor(resources, id) }

fun Activity.lazyPixel(@DimenRes id: Int): Lazy<Int> = lazy { resources.getDimensionPixelSize(id) }

fun Activity.lazyColor(@ColorRes id: Int): Lazy<Int> = lazy { ContextCompat.getColor(this, id) }

inline operator fun <reified T : View> Activity.get(@LayoutRes id: Int): T = findViewById(id) as T

inline fun <reified T : View> Activity.lazyView(@LayoutRes id: Int): Lazy<T> = lazy { findViewById(id) as T }

fun Activity.shortToast(@StringRes textId: Int) = Toast.makeText(this, textId, Toast.LENGTH_SHORT).show()

fun Activity.shortToast(text: CharSequence) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

fun Activity.shortToast(@StringRes textId: Int, vararg args: Any?) {
    Toast.makeText(this, getString(textId, *args), Toast.LENGTH_SHORT).show()
}

fun Activity.longToast(@StringRes textId: Int) = Toast.makeText(this, textId, Toast.LENGTH_SHORT).show()

fun Activity.longToast(text: CharSequence) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

fun Activity.longToast(@StringRes textId: Int, vararg args: Any?) {
    Toast.makeText(this, getString(textId, *args), Toast.LENGTH_SHORT).show()
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

val Activity.statusHeight: Int
    get() = resources.getDimensionPixelSize(resources.getIdentifier("status_bar_height", "dimen", "android"))

val Activity.navigationHeight: Int
    get() = resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_height", "dimen", "android"))

fun Activity.setStatusBarColor(@ColorInt color: Int): Boolean {
    val window = window
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.statusBarColor = color
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        fitsSystemWindows()
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        (window.decorView as ViewGroup).addView(coloredView(this, color, MATCH_PARENT, statusHeight))
    } else {
        return false
    }
    return true
}

fun Activity.setStatusBarTranslucent(): Boolean {
    val window = window
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    } else {
        return false
    }
    return true
}

fun Activity.setStatusBarLight(darker: Boolean): Boolean {
    val window = window
    if (setMiuiDarker(window, darker) || setFlymeDarker(window, darker)) {
        return true
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val decorView = window.decorView
        var flags = decorView.systemUiVisibility
        if (darker) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
        decorView.systemUiVisibility = flags
    } else {
        return false
    }
    return true
}

fun Activity.fitsSystemWindows() {
    val view = (findViewById(android.R.id.content) as ViewGroup).getChildAt(0)
    if (view is DrawerLayout) {
        view.getChildAt(0).fitsSystemWindows = true
    } else {
        view.fitsSystemWindows = true
    }
}

private fun setFlymeDarker(window: Window, darker: Boolean): Boolean {
    var result = false
    try {
        val params = window.attributes
        val darkFlag = WindowManager.LayoutParams::class.java.getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON")
        val meizuFlags = WindowManager.LayoutParams::class.java.getDeclaredField("meizuFlags")
        darkFlag.isAccessible = true
        meizuFlags.isAccessible = true
        val bit = darkFlag.getInt(null)
        val value = meizuFlags.getInt(params)
        meizuFlags.setInt(params, if (darker) value or bit else value and bit.inv())
        window.attributes = params
        result = true
    } catch (ignored: Exception) {
    }
    return result
}

private fun setMiuiDarker(window: Window, darker: Boolean): Boolean {
    var result = false
    try {
        val clazz = Class.forName("android.view.MiuiWindowManager\$LayoutParams")
        val flag = clazz.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE").getInt(null)
        val method = window.javaClass.getMethod("setExtraFlags", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        method.invoke(window, if (darker) flag else 0, flag)
        result = true
    } catch (ignored: Exception) {
    }
    return result
}
