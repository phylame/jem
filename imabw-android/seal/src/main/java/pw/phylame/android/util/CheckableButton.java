package pw.phylame.android.util;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageButton;

public class CheckableButton extends ImageButton {
    public int normalIconId;

    public int selectedIconId;

    public CheckableButton(Context context) {
        super(context);
    }

    public CheckableButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckableButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CheckableButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        setImageResource(selected ? selectedIconId : normalIconId);
    }
}
