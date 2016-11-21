package pw.phylame.seal;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

import lombok.val;

class ItemFilter implements FileFilter {
    private final boolean fileMode;
    private final Pattern pattern;

    boolean showHidden;

    ItemFilter(boolean fileMode, Pattern pattern, boolean showHidden) {
        this.fileMode = fileMode;
        this.pattern = pattern;
        this.showHidden = showHidden;
    }

    @Override
    public boolean accept(File file) {
        if (!fileMode && file.isFile()) {
            return false;
        }

        val name = file.getName();

        if (!showHidden && IOs.isHidden(name)) {
            return false;
        }

        if (pattern != null) {
            if ((fileMode && file.isFile()) || (!fileMode && file.isDirectory())) {
                return pattern.matcher(name).matches();
            }
        }

        return true;
    }
}
