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

package pw.phylame.jem.formats.pmab;

import pw.phylame.jem.epm.base.ZipOutConfig;
import pw.phylame.jem.epm.util.config.Configured;
import pw.phylame.jem.epm.util.xml.XmlConfig;

import java.util.Map;

/**
 * Config for making PMAB book.
 */
public class PmabOutConfig extends ZipOutConfig {
    public static final String SELF = "config";

    public static final String VERSION = "version";
    public static final String TEXT_DIR = "textDir";
    public static final String IMAGE_DIR = "imageDir";
    public static final String EXTRA_DIR = "extraDir";
    public static final String XML_CONFIG = "xmlConfig";
    public static final String TEXT_ENCODING = "encoding";
    public static final String DATE_FORMAT = "dateFormat";
    public static final String META_INFO = "metadata";

    /**
     * Output PMAB version
     */
    @Configured(VERSION)
    public String version = "3.0";

    /**
     * Directory in PMAB for storing text.
     */
    @Configured(TEXT_DIR)
    public String textDir = "text";

    /**
     * Directory in PMAB for storing images.
     */
    @Configured(IMAGE_DIR)
    public String imageDir = "images";

    /**
     * Directory in PMAB for storing extra file(s).
     */
    @Configured(EXTRA_DIR)
    public String extraDir = "extras";

    /**
     * XML render config.
     */
    @Configured(XML_CONFIG)
    public XmlConfig xmlConfig = new XmlConfig();

    /**
     * Encoding for converting all text in PMAB.
     */
    @Configured(TEXT_ENCODING)
    public String textEncoding = PMAB.defaultEncoding;

    /**
     * Format for storing <tt>Date</tt> value.
     */
    @Configured(DATE_FORMAT)
    public String dateFormat = "yyyy-M-d";

    /**
     * Addition information to PMAB archive.
     * <p><strong>NOTE:</strong> The key and value stored as String.
     */
    @Configured(META_INFO)
    public Map<Object, Object> metadata = null;

    @Override
    public void adjust() {
        xmlConfig.standalone = true;
    }
}
