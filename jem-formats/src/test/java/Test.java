import java.io.File;
import java.io.IOException;

import lombok.val;
import pw.phylame.jem.formats.pmab.PmabParser;
import pw.phylame.jem.util.JemException;

/**
 * Created by phyla on 2016/11/22.
 */
public class Test {
    public static void main(String[] args) throws IOException, JemException {
        val book = new PmabParser().parse(new File("E:\\tmp\\sy.pmab"), null);
        System.out.println(book);
    }
}
