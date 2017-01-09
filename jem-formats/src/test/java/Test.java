import lombok.val;
import pw.phylame.jem.epm.EpmManager;
import pw.phylame.jem.util.JemException;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.log.LogLevel;

import java.io.IOException;

import static pw.phylame.jem.epm.util.DebugUtils.*;

public class Test {
    public static void main(String[] args) throws IOException, JemException {
        System.setProperty(EpmManager.AUTO_LOAD_CUSTOMIZED_KEY, "true");
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
