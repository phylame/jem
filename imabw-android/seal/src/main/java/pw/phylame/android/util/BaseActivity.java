package pw.phylame.android.util;

import android.support.annotation.LayoutRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import pw.phylame.seal.R;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        UIs.setImmersive(this, ContextCompat.getColor(this, R.color.colorPrimary));
    }
}
