/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This raf is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this raf except in compliance with the License.
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

package pw.phylame.jem.formats.txt;


import pw.phylame.jem.epm.util.config.AbstractConfig;
import pw.phylame.jem.epm.util.config.Configured;
import pw.phylame.jem.epm.util.text.TextConfig;

/**
 * Config for making TXT book.
 */
public class TxtOutConfig extends AbstractConfig {
    public static final String SELF = "config";
    public static final String TEXT_CONFIG = "textConfig";
    public static final String ENCODING = "encoding";
    public static final String HEADER = "header";
    public static final String ADDITION_LINE = "additionLine";
    public static final String FOOTER = "footer";

    /**
     * Render config for rendering book text.
     *
     * @see TextConfig
     */
    @Configured(TEXT_CONFIG)
    public TextConfig textConfig = new TextConfig();

    /**
     * Encoding for converting book text.
     */
    @Configured(ENCODING)
    public String encoding = TXT.defaultEncoding;

    /**
     * Text appended to header of TXT raf.
     */
    @Configured(HEADER)
    public String header = null;

    /**
     * Add addition end line separator for each chapter
     */
    @Configured(ADDITION_LINE)
    public boolean additionLine = true;

    /**
     * Text appended to footer of TXT raf.
     */
    @Configured(FOOTER)
    public String footer = null;
}
