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

package pw.phylame.jem.formats.util.html;

import lombok.val;
import pw.phylame.jem.formats.util.JFMessages;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.flob.Flobs;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * CSS config for rendering HTML.
 */
public class StyleProvider {

    public Flob cssFile;

    public String bookCover;

    public String bookTitle;
    public String introTitle;
    public String introText;

    public String tocTitle;
    public String tocItems;

    public String sectionCover;
    public String sectionTitle;
    public String sectionIntro;
    public String sectionItems;

    public String chapterCover;
    public String chapterTitle;
    public String chapterIntro;
    public String chapterText;

    private static StyleProvider defaultInstance = null;

    public static StyleProvider getDefaults() throws IOException {
        if (defaultInstance == null) {
            loadDefaultInstance();
        }
        return defaultInstance;
    }

    public static final String CONFIG_FILE = "default-styles.properties";

    private static void loadDefaultInstance() throws IOException {
        defaultInstance = new StyleProvider();
        val in = StyleProvider.class.getResourceAsStream(CONFIG_FILE);
        if (in == null) {
            throw new IOException(JFMessages.tr("err.html.loadStyle", CONFIG_FILE));
        }
        val prop = new Properties();
        try {
            prop.load(in);
            fetchStyles(prop);
        } finally {
            in.close();
        }
    }

    private static void fetchStyles(Properties prop) throws IOException {
        String cssPath = prop.getProperty("url");
        URL url;
        if (cssPath.startsWith(":")) {   // in JAR
            url = StyleProvider.class.getResource(cssPath.substring(1));
        } else {
            url = new URL(cssPath);
        }
        defaultInstance.cssFile = Flobs.forURL(url, "text/css");

        defaultInstance.bookCover = prop.getProperty("bookCover");

        defaultInstance.bookTitle = prop.getProperty("bookTitle");
        defaultInstance.introTitle = prop.getProperty("introTitle");
        defaultInstance.introText = prop.getProperty("introText");

        defaultInstance.tocTitle = prop.getProperty("tocTitle");
        defaultInstance.tocItems = prop.getProperty("tocItems");

        defaultInstance.sectionCover = prop.getProperty("sectionCover");
        defaultInstance.sectionTitle = prop.getProperty("sectionTitle");
        defaultInstance.sectionIntro = prop.getProperty("sectionIntro");
        defaultInstance.sectionItems = prop.getProperty("sectionItems");

        defaultInstance.chapterCover = prop.getProperty("chapterCover");
        defaultInstance.chapterTitle = prop.getProperty("chapterTitle");
        defaultInstance.chapterIntro = prop.getProperty("chapterIntro");
        defaultInstance.chapterText = prop.getProperty("chapterText");
    }
}
