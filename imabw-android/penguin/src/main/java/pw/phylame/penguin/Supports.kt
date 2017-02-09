package pw.phylame.penguin

import android.support.v4.content.ContextCompat
import pw.phylame.ancotols.ManagedActivity
import pw.phylame.ancotols.setStatusBarColor

abstract class BaseActivity : ManagedActivity() {
    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary))
    }
}
