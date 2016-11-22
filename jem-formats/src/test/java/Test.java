import lombok.val;
import pw.phylame.jem.formats.umd.UmdParser;
import pw.phylame.jem.util.JemException;

import java.io.File;
import java.io.IOException;

/**
 * Created by phyla on 2016/11/22.
 */
public class Test {
    public static void main(String[] args) throws IOException, JemException {
        val book = new UmdParser().parse(new File("D:\\down\\qiyu\\187478.umd"), null);
        System.out.println(book.size());
    }
}
