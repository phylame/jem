package pw.phylame.imabw

import android.app.Activity
import android.app.Application
import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatEditText
import android.view.View
import android.widget.EditText
import jem.epm.EpmManager
import pw.phylame.ancotols.ManagedActivity
import pw.phylame.ancotols.get
import pw.phylame.ancotols.setStatusBarColor

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        System.setProperty(EpmManager.AUTO_LOAD_KEY, "true")
    }
}

internal abstract class BaseActivity : ManagedActivity() {
    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary))
    }
}

fun Context.message(title: CharSequence, message: CharSequence) {
    AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.button_ok, null)
            .create()
            .show()
}

fun Context.message(titleId: Int, messageId: Int, vararg args: Any?) {
    AlertDialog.Builder(this)
            .setTitle(titleId)
            .setMessage(getString(messageId, *args))
            .setPositiveButton(R.string.button_ok, null)
            .create()
            .show()
}

fun Context.confirm(titleId: Int,
                    messageId: Int,
                    okId: Int = R.string.button_ok,
                    cancelId: Int = R.string.button_cancel, action: (Boolean) -> Unit) {
    AlertDialog.Builder(this)
            .setTitle(titleId)
            .setMessage(messageId)
            .setPositiveButton(okId) { dialog, which ->
                action(true)
            }
            .setNegativeButton(cancelId) { dialog, which ->
                action(false)
            }
            .create()
            .show()
}

fun Activity.input(titleId: Int,
                   initial: CharSequence = "",
                   hintId: Int = -1,
                   singleLine: Boolean = false,
                   action: (String) -> Unit) {
    val (view, text) = initInput(this, initial, hintId, singleLine)
    AlertDialog.Builder(this)
            .setTitle(titleId)
            .setView(view)
            .setPositiveButton(R.string.button_ok) { dialog, which ->
                action(text.text.toString())
            }
            .setNegativeButton(R.string.button_cancel, null)
            .create()
            .show()
}

fun Activity.input(title: CharSequence,
                   initial: CharSequence = "",
                   hintId: Int = -1,
                   singleLine: Boolean = false,
                   action: (String) -> Unit) {
    val (view, text) = initInput(this, initial, hintId, singleLine)
    AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(R.string.button_ok) { dialog, which ->
                action(text.text.toString())
            }
            .setNegativeButton(R.string.button_cancel, null)
            .create()
            .show()
}

fun Activity.datetime() {
    val view = View.inflate(this, R.layout.dialog_datetime, null)
    AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(R.string.button_ok) { dialog, which ->

            }
            .setNegativeButton(R.string.button_cancel, null)
            .create()
            .show()
}

private fun initInput(context: Context, initial: CharSequence, hintId: Int, singleLine: Boolean): Pair<View, EditText> {
    val view = View.inflate(context, R.layout.dialog_input, null)
    val text: AppCompatEditText = view[R.id.text]
    text.setText(initial)
    if (hintId > 0) {
        text.setHint(hintId)
    }
    text.setSingleLine(singleLine)
    text.setSelection(0, initial.length)
    return view to text
}

fun Context.getQuantityString(id: Int, quantity: Int): String = resources.getQuantityString(id, quantity)

fun Context.getQuantityString(id: Int, quantity: Int, vararg args: Any): String = if (args.isNotEmpty()) {
    resources.getQuantityString(id, quantity, *args)
} else {
    resources.getQuantityString(id, quantity)
}
