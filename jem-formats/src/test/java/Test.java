import static pw.phylame.jem.epm.util.DebugUtils.makeFile;
import static pw.phylame.jem.epm.util.DebugUtils.parseFile;
import static pw.phylame.jem.epm.util.DebugUtils.printAttributes;
import static pw.phylame.jem.epm.util.DebugUtils.printExtension;

import java.io.IOException;

import lombok.val;
import pw.phylame.jem.epm.EpmManager;
import pw.phylame.jem.util.JemException;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.log.LogLevel;

/**
 * Created by phyla on 2016/11/22.
 */
public class Test {
    public static void main(String[] args) throws IOException, JemException {
        System.setProperty(EpmManager.AUTO_LOAD_CUSTOMIZED_KEY, "true");
        Log.setLevel(LogLevel.ALL);
        val book = parseFile("D:/down/qiyu/yxz.mobi", null);
        if (book == null) {
            return;
        }
        printAttributes(book, true);
        printExtension(book, true);
        makeFile(book,"E:/tmp/abc.pmab",null);
    }
}
