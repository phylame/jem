package pw.phylame.seal;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageButton;

public class CheckedImageButton extends ImageButton {
    public int normalIconId;

    public int selectedIconId;

    public CheckedImageButton(Context context) {
        super(context);
    }

    public CheckedImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckedImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CheckedImageButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        setImageResource(selected ? selectedIconId : normalIconId);
    }
}
