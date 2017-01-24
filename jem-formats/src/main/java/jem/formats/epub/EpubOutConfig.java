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

package jem.formats.epub;

import jem.epm.impl.ZipMaker.ZipOutConfig;
import jem.epm.util.config.Configured;
import jem.epm.util.xml.XmlConfig;
import jem.formats.util.html.HtmlConfig;

/**
 * Config for making ePub book.
 */
public class EpubOutConfig extends ZipOutConfig {
    public static final String SELF = "config";
    public static final String VERSION = "version";
    public static final String XML_CONFIG = "xmlConfig";
    public static final String HTML_CONFIG = "htmlConfig";
    public static final String UUID = "uuid";
    public static final String UUID_TYPE = "uuidType";
    public static final String DATE_FORMAT = "dateFormat";
    public static final String SMALL_PAGE = "smallPage";

    /**
     * Output ePub version.
     */
    @Configured(VERSION)
    public String version = "2.0";

    @Configured(XML_CONFIG)
    public XmlConfig xmlConfig = new XmlConfig();

    @Configured(HTML_CONFIG)
    public HtmlConfig htmlConfig = new HtmlConfig();

    @Configured(UUID)
    public String uuid = null;

    @Configured(UUID_TYPE)
    public String uuidType = "uuid";

    @Configured(DATE_FORMAT)
    public String dateFormat = EPUB.dateFormat;

    /**
     * If <tt>smallPage</tt> is <tt>true</tt>, each HTML page will be smaller for mobile version.
     */
    @Configured(SMALL_PAGE)
    public boolean smallPage = true;

    @Override
    public void adjust() {
        xmlConfig.standalone = true;
    }
}
