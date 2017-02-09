package pw.phylame.ancotols

import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.support.v4.os.EnvironmentCompat
import java.io.File
import java.io.FileFilter

object IOs {
    var sizeSuffix: CharSequence = "B"

    fun statOf(storage: String): Pair<Long, Long> {
        val stat = StatFs(storage)
        val free: Long
        val total: Long
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            free = stat.availableBytes
            total = stat.totalBytes
        } else {
            val size = stat.blockSize
            free = (size * stat.availableBlocks).toLong()
            total = (size * stat.blockCount).toLong()
        }
        return total to free
    }

    fun readable(size: Long): String = if (size < 0x400) {
        "$size $sizeSuffix"
    } else if (size < 0x100000) {
        String.format("%.2f K%s", size / 1024.0, sizeSuffix)
    } else if (size < 0x40000000) {
        String.format("%.2f M%s", size.toDouble() / 1024.0 / 1024.0, sizeSuffix)
    } else {
        String.format("%.2f G%s", size.toDouble() / 1024.0 / 1024.0 / 1024.0, sizeSuffix)
    }

    fun itemsOf(dir: File, filter: FileFilter? = null): Array<File> = dir.listFiles(filter) ?: emptyArray()

    fun countOf(dir: File, filter: FileFilter? = null): Int {
        val items = dir.list() ?: return 0
        if (filter == null) {
            return items.size
        }
        return items.count { filter.accept(File(dir, it)) }
    }

    val sdcardPath: String by lazy {
        System.getenv("SECONDARY_STORAGE") ?: ""
    }

    val otgPath: String by lazy {
        val path = System.getenv("ANDROID_STORAGE")
        if (path != null && path.isNotEmpty()) {
            File(path).list()?.firstOrNull { "otg" in it } ?: ""
        } else ""
    }

    fun isMounted(path: String): Boolean {
        return Environment.MEDIA_MOUNTED == getStorageState(path)
    }

    fun isUnmounted(path: String): Boolean {
        return Environment.MEDIA_UNMOUNTED == getStorageState(path)
    }

    fun getStorageState(path: String): String {
        return EnvironmentCompat.getStorageState(File(path))
    }
}
