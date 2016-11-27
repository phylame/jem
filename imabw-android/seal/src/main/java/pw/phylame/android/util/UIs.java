package pw.phylame.android.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.InputStream;

import lombok.val;
import pw.phylame.seal.R;

public final class UIs {
    private static TypedValue mReuseValue = new TypedValue();

    public static float dimenFor(Resources resources, int id) {
        val value = mReuseValue;
        resources.getValue(id, value, true);
        return TypedValue.complexToFloat(value.data);
    }

    public static void showProgress(Context context, final View progressBar, final boolean shown) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
            progressBar.setVisibility(shown ? View.VISIBLE : View.GONE);
            progressBar.animate()
                    .setDuration(shortAnimTime)
                    .alpha(shown ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            progressBar.setVisibility(shown ? View.VISIBLE : View.GONE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressBar.setVisibility(shown ? View.VISIBLE : View.GONE);
        }
    }

    public static void alert(Context context, Integer titleId, Integer messageId) {
        val builder = new AlertDialog.Builder(context);
        if (titleId != null) {
            builder.setTitle(titleId);
        }
        if (messageId != null) {
            builder.setMessage(messageId);
        }
        builder
                .setPositiveButton(R.string.button_ok, new CloseDialogListener())
                .create()
                .show();
    }

    public static void alert(Context context, String title, String message) {
        val builder = new AlertDialog.Builder(context);
        if (title != null) {
            builder.setTitle(title);
        }
        if (message != null) {
            builder.setMessage(message);
        }
        builder
                .setPositiveButton(R.string.button_ok, new CloseDialogListener())
                .create()
                .show();
    }

    public static void shortToast(Context context, int resId) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT)
                .show();
    }

    public static void shortToast(Context context, CharSequence text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT)
                .show();
    }

    public static void longToast(Context context, int resId) {
        Toast.makeText(context, resId, Toast.LENGTH_LONG)
                .show();
    }

    public static void longToast(Context context, CharSequence text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG)
                .show();
    }

    public static Drawable drawableOf(InputStream in, int width, int height) {
        val bmp = BitmapFactory.decodeStream(in);
        if (width <= 0 && height <= 0) {
            return new BitmapDrawable(null, bmp);
        }

        int ow = bmp.getWidth(), oh = bmp.getHeight();

        Matrix matrix = new Matrix();
        if (width <= 0) {
            float hs = (float) height / oh;
            matrix.postScale(hs, hs);
        } else if (height <= 0) {
            float ws = (float) width / ow;
            matrix.postScale(ws, ws);
        } else {
            matrix.postScale((float) width / ow, (float) height / oh);
        }

        return new BitmapDrawable(null, Bitmap.createBitmap(bmp, 0, 0, ow, oh, matrix, true));
    }

    public static void setImmersive(Activity activity, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val decor = (ViewGroup) adjustWindowFlags(activity).getDecorView();
            decor.addView(createStatusView(activity, color));
            adjustRootView(activity);
        }
    }

    public static void setImmersive(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            adjustWindowFlags(activity);
            adjustRootView(activity);
        }
    }

    public static void setImmersive(Activity activity, DrawerLayout drawer, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            adjustWindowFlags(activity);

            val content = (ViewGroup) drawer.getChildAt(0);
            content.addView(createStatusView(activity, color));

            val first = content.getChildAt(1);
            if (!(content instanceof LinearLayout) && first != null) {
                first.setPadding(0, getStatusBarHeight(activity), 0, 0);
            }

            drawer.setFitsSystemWindows(true);
            content.setFitsSystemWindows(true);
            drawer.getChildAt(1)
                    .setFitsSystemWindows(true);
        }
    }

    public static void setImmersive(Activity activity, DrawerLayout drawer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            adjustWindowFlags(activity);

            drawer.setFitsSystemWindows(false);

            val content = (ViewGroup) drawer.getChildAt(0);
            content.setFitsSystemWindows(true);
            content.setClipToPadding(true);

            drawer.getChildAt(1)
                    .setFitsSystemWindows(false);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static Window adjustWindowFlags(Activity activity) {
        val window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        return window;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void adjustRootView(Activity activity) {
        val root = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        root.setFitsSystemWindows(true);
    }

    public static int getStatusBarHeight(Activity activity) {
        val resources = activity.getResources();
        int id = resources.getIdentifier("status_bar_height", "dimen", "android");
        return resources.getDimensionPixelSize(id);
    }

    public static int getNavigationBarHeight(Activity activity) {
        val resources = activity.getResources();
        int id = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        return resources.getDimensionPixelSize(id);
    }

    private static View createStatusView(Activity activity, int color) {
        val view = new View(activity);
        val height = getStatusBarHeight(activity);
        view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));
        view.setBackgroundColor(color);
        return view;
    }

    private static View createNavigationView(Activity activity, int color) {
        val view = new View(activity);
        val height = getNavigationBarHeight(activity);
        view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));
        view.setBackgroundColor(color);
        return view;
    }

    private static class CloseDialogListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
        }
    }
}
