/*
 * Copyright 2016 Peng Wan <phylame@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.epm.Helper;
import pw.phylame.jem.epm.Registry;
import pw.phylame.jem.formats.jar.JarOutConfig;
import pw.phylame.jem.formats.pmab.PmabOutConfig;
import pw.phylame.jem.util.JemException;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.jem.util.text.Text;
import pw.phylame.jem.util.text.Texts;
import pw.phylame.ycl.util.MiscUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class Test {
    public static void main(String[] args) throws IOException, JemException {
        System.setProperty(Registry.AUTO_LOAD_CUSTOMIZED_KEY, "true");
        System.out.println(Arrays.toString(Registry.supportedMakers()));
        System.out.println(Arrays.toString(Registry.supportedParsers()));
        val book = new Book("Example", "PW");
        book.setText(Texts.forString("My name is PW\n, and your?", Text.PLAIN));
        Attributes.setCover(book, Flobs.forURL(new URL("https://www.baidu.com/img/2016_8_17chinabetter_3fdfb7e62eab01c73e39ee3e9751a0e5.gif"), null));
        Attributes.setPubdate(book, new Date());
        Attributes.setLanguage(book, Locale.getDefault());
        Attributes.setWords(book, 5000000);
        Attributes.setIntro(book, Texts.forString("Hello world", Text.PLAIN));
        book.append(book.clone());
        val path = "d:\\tmp\\ex.pmab";
        val format = "jar";
        Map<String, Object> arguments = MiscUtils.mapOf(
                "pmab.make." + PmabOutConfig.VERSION, "2.0",
                "jar.make." + JarOutConfig.JAR_TEMPLATE, "D:\\code\\java\\pw-books\\jem-formats\\src\\main\\resources\\pw\\phylame\\jem\\formats\\jar\\book.jar"
        );
//        Helper.writeBook(book, new File(path), format, arguments);
        System.out.println(Helper.readBook(new File("E:\\books\\小说\\武侠\\凤歌\\沧海（新版）.pmab"), "pmab", null));
    }
}
