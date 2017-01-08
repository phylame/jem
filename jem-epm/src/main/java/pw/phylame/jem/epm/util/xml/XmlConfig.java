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

package pw.phylame.jem.epm.util.xml;

import pw.phylame.jem.epm.util.config.AbstractConfig;
import pw.phylame.jem.epm.util.config.Configured;

/**
 * Config for rendering XML.
 */
public class XmlConfig extends AbstractConfig {
    public static final String SELF = "xml.config";
    public static final String ENCODING = "xml.encoding";
    public static final String STANDALONE = "xml.standalone";
    public static final String LINE_SEPARATOR = "xml.lineSeparator";
    public static final String INDENT_STRING = "xml.indentString";

    @Configured(ENCODING)
    public String encoding = "UTF-8";

    @Configured(STANDALONE)
    public boolean standalone = true;

    @Configured(LINE_SEPARATOR)
    public String lineSeparator = "\n";

    @Configured(INDENT_STRING)
    public String indentString = "\t";
}
