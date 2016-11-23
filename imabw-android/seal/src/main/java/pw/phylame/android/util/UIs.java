package pw.phylame.android.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import lombok.val;
import pw.phylame.seal.R;

public final class UIs {
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
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        return window;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void adjustRootView(Activity activity) {
        val root = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        root.setFitsSystemWindows(true);
    }

    private static int getStatusBarHeight(Activity activity) {
        val resources = activity.getResources();
        int id = resources.getIdentifier("status_bar_height", "dimen", "android");
        return resources.getDimensionPixelSize(id);
    }

    private static int getNavigationBarHeight(Activity activity) {
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
