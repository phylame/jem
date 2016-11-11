/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
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
import pw.phylame.jem.formats.util.M;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.util.CollectUtils;
import pw.phylame.ycl.util.StringUtils;

import java.io.IOException;
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

    private static volatile StyleProvider defaultInstance = null;

    public static StyleProvider getDefaults() throws IOException {
        if (defaultInstance == null) {
            synchronized (StyleProvider.class) {
                if (defaultInstance == null) {
                    loadDefaultInstance();
                }
            }
        }
        return defaultInstance;
    }

    public static final String CONFIG_FILE = "!pw/phylame/jem/formats/util/html/default-styles.properties";

    private static void loadDefaultInstance() throws IOException {
        defaultInstance = new StyleProvider();
        val prop = CollectUtils.propertiesFor(CONFIG_FILE);
        if (prop == null) {
            throw new IOException(M.tr("err.html.loadStyle", CONFIG_FILE));
        }
        fetchStyles(defaultInstance, prop);
    }

    public static void fetchStyles(StyleProvider provider, Properties prop) throws IOException {
        val cssPath = prop.getProperty("url");
        val url = IOUtils.resourceFor(cssPath);
        if (url == null) {
            throw new IOException(M.tr("err.html.noCSS", cssPath));
        }
        provider.cssFile = Flobs.forURL(url, "text/css");

        try {
            for (val field : provider.getClass().getFields()) {
                if (!CharSequence.class.isAssignableFrom(field.getType())) {
                    return;
                }
                val value = prop.getProperty(field.getName());
                if (StringUtils.isNotEmpty(value)) {
                    field.set(provider, value);
                }
            }
        } catch (IllegalAccessException e) {
            throw new InternalError(e.getMessage());
        }
    }
}
