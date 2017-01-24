/*
 * Copyright 2014-2017 Peng Wan <phylame@163.com>
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

package jem.formats.util.html;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Properties;

import jem.formats.util.M;
import jem.util.flob.Flob;
import jem.util.flob.Flobs;
import lombok.val;
import pw.phylame.commons.function.Provider;
import pw.phylame.commons.io.IOUtils;
import pw.phylame.commons.util.CollectionUtils;
import pw.phylame.commons.util.StringUtils;
import pw.phylame.commons.value.Lazy;

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

    private static final Lazy<StyleProvider> defaults = new Lazy<>(new Provider<StyleProvider>() {
        @Override
        public StyleProvider provide() throws Exception {
            return loadDefaultInstance();
        }
    });

    public static StyleProvider getDefaults() {
        return defaults.get();
    }

    public static final String CONFIG_FILE = "!jem/formats/util/html/default-styles.properties";

    private static StyleProvider loadDefaultInstance() throws IOException {
        val provider = new StyleProvider();
        val prop = CollectionUtils.propertiesFor(CONFIG_FILE);
        if (prop == null) {
            throw new IOException(M.tr("err.html.loadStyle", CONFIG_FILE));
        }
        fetchStyles(provider, prop);
        return provider;
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
                if (!Modifier.isPublic(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!CharSequence.class.isAssignableFrom(field.getType())) {
                    continue;
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
