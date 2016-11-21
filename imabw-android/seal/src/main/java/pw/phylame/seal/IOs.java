package pw.phylame.seal;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.os.EnvironmentCompat;
import android.util.Pair;

import java.io.File;
import java.io.FileFilter;

import lombok.val;

public final class IOs {
    public static Pair<Integer, Integer> split(String path) {
        int extpos = path.length(), seppos;
        char ch;
        for (seppos = extpos - 1; seppos >= 0; --seppos) {
            ch = path.charAt(seppos);
            if (ch == '.') {
                extpos = seppos;
            } else if (ch == '/' || ch == '\\') {
                break;
            }
        }
        return new Pair<>(seppos, extpos);
    }

    public static String fullName(String path) {
        int seppos = split(path).first;
        return path.substring(seppos != 0 ? seppos + 1 : seppos);
    }

    public static String baseName(String path) {
        val pair = split(path);
        return path.substring(pair.first + 1, pair.second);
    }

    public static String extensionName(String path) {
        int extsep = split(path).second;
        return extsep != path.length() ? path.substring(extsep + 1) : "";
    }

    @SuppressWarnings("deprecation")
    public static Pair<Long, Long> statOf(String storage) {
        val stat = new StatFs(storage);
        long free, total;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            free = stat.getAvailableBytes();
            total = stat.getTotalBytes();
        } else {
            val size = stat.getBlockSize();
            free = size * stat.getAvailableBlocks();
            total = size * stat.getBlockCount();
        }
        return new Pair<>(total, free);
    }

    public static boolean isHidden(String name) {
        return name.indexOf('.') == 0;
    }

    public static String readableSize(long size, CharSequence suffix) {
        if (size < 0x400) {
            return size + " " + suffix;
        } else if (size < 0x100000) {
            return String.format("%.2f K" + suffix, size / 1024.0);
        } else if (size < 0x40000000) {
            return String.format("%.2f M" + suffix, size / 1024.0 / 1024.0);
        } else {
            return String.format("%.2f G" + suffix, size / 1024.0 / 1024.0 / 1024.0);
        }
    }

    public static File[] itemsOf(File dir, FileFilter filter) {
        val files = dir.listFiles(filter);
        return files == null ? new File[0] : files;
    }

    private static String sdcardPath = null;

    public static String getSDCardPath() {
        if (sdcardPath == null) {
            sdcardPath = System.getenv("SECONDARY_STORAGE");
        }
        return sdcardPath;
    }

    private static String otgPath = null;

    public static String getOTGPath() {
        if (otgPath == null) {
            val paths = new File(System.getenv("ANDROID_STORAGE")).list();
            if (paths != null) {
                for (val path : paths) {
                    if (path.contains("otg")) {
                        otgPath = path;
                        break;
                    }
                }
            }
        }
        return otgPath;
    }

    public static boolean isMounted(String path) {
        return Environment.MEDIA_MOUNTED.equals(getStorageState(path));
    }

    public static boolean isUnmounted(String path) {
        return Environment.MEDIA_UNMOUNTED.equals(getStorageState(path));
    }

    public static String getStorageState(String path) {
        return EnvironmentCompat.getStorageState(new File(path));
    }
}
