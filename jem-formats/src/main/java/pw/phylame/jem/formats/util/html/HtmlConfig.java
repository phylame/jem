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

import pw.phylame.jem.epm.util.config.AbstractConfig;
import pw.phylame.jem.epm.util.config.Configured;

import java.util.Map;

/**
 * Config for rendering HTML.
 */
public class HtmlConfig extends AbstractConfig {
    public static final String SELF = "html.config";
    public static final String ENCODING = "html.encoding";
    public static final String INDENT_STRING = "html.indentString";
    public static final String META_INFO = "html.metaInfo";
    public static final String STYLE_PROVIDER = "html.styleProvider";
    public static final String SKIP_EMPTY_LINE = "html.skipEmptyLine";

    /**
     * Encoding for writing HTML.
     */
    @Configured(ENCODING)
    public String encoding = "UTF-8";

    /**
     * HTML indent string.
     */
    @Configured(INDENT_STRING)
    public String indentString = "\t";

    /**
     * Value of attribute xml:lang.
     */
    public String htmlLanguage;

    /**
     * Href of CSS file.
     * <p>You need save CSS file firstly then get the href.
     */
    public String cssHref;

    /**
     * Addition messages to HTML head->meta element.
     */
    @Configured(META_INFO)
    public Map<String, String> metaInfo;

    /**
     * HTML CSS config.
     */
    @Configured(STYLE_PROVIDER)
    public StyleProvider style;

    /**
     * When making paragraph skip empty line of {@code Text}.
     */
    @Configured(SKIP_EMPTY_LINE)
    public boolean skipEmpty = true;
}
