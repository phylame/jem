import lombok.val;
import pw.phylame.commons.log.Log;
import pw.phylame.commons.log.LogLevel;

import static jem.epm.util.DebugUtils.*;

import java.io.IOException;

import jem.epm.EpmManager;
import jem.util.JemException;

public class Test {
    public static void main(String[] args) throws IOException, JemException {
        System.setProperty(EpmManager.AUTO_LOAD_KEY, "true");
        Log.setLevel(LogLevel.ALL);
        val book = parseFile("F:/books/恐慌沸腾.pmab.zip", "pmab", null);
        if (book == null) {
            return;
        }
        printAttributes(book, true);
        printExtension(book, true);
        makeFile(book, "E:/tmp/abc.epub", null);
    }
}
