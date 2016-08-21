/*
 * Copyright 2014-2015 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.formats.epub;

import pw.phylame.jem.epm.base.ZipOutConfig;
import pw.phylame.jem.epm.util.config.Mapped;
import pw.phylame.jem.epm.util.xml.XmlConfig;
import pw.phylame.jem.formats.util.html.HtmlConfig;

/**
 * Config for making ePub book.
 */
public class EpubOutConfig extends ZipOutConfig {
    public static final String SELF = "config";
    public static final String VERSION = "version";
    public static final String XML_CONFIG = "xmlConfig";
    public static final String HTML_CONFIG = "htmlConfig";
    public static final String UUID = "uuid";
    public static final String DATE_FORMAT = "dateFormat";
    public static final String SMALL_PAGE = "smallPage";

    /**
     * Output ePub version.
     */
    @Mapped(VERSION)
    public String version = "2.0";

    @Mapped(XML_CONFIG)
    public XmlConfig xmlConfig = new XmlConfig();

    @Mapped(HTML_CONFIG)
    public HtmlConfig htmlConfig = new HtmlConfig();

    @Mapped(UUID)
    public String uuid = null;

    @Mapped(DATE_FORMAT)
    public String dateFormat = EPUB.dateFormat;

    /**
     * If <tt>smallPage</tt> is <tt>true</tt>, each HTML page will be smaller.
     */
    @Mapped(SMALL_PAGE)
    public boolean smallPage = true;

    @Override
    public void adjust() {
        xmlConfig.standalone = true;
    }
}
