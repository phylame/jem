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

package pw.phylame.jem.epm.util.text;

import pw.phylame.jem.epm.util.JEMessages;
import pw.phylame.jem.epm.util.config.AbstractConfig;
import pw.phylame.jem.epm.util.config.Mapped;

/**
 * Config for rendering text.
 */
public class TextConfig extends AbstractConfig {
    public static final String SELF = "text.render.config";  // RenderConfig
    public static final String WRITE_TITLE = "text.render.writeTitle";  // boolean
    public static final String JOIN_TITLES = "text.render.joinTitles";  // boolean
    public static final String PREFIX_TEXT = "text.render.prefixText";  // String
    public static final String TITLE_SEPARATOR = "text.render.titleSeparator";  // String
    public static final String WRITE_INTRO = "text.render.writeIntro";  // boolean
    public static final String INTRO_SEPARATOR = "text.render.introSeparator";  // String
    public static final String FORMAT_PARAGRAPH = "text.render.formatParagraph";    // boolean
    public static final String PARAGRAPH_PREFIX = "text.render.paragraphPrefix";    // String
    public static final String SKIP_EMPTY_LINE = "text.render.skipEmptyLine";   // boolean
    public static final String LINE_SEPARATOR = "text.render.lineSeparator";    // String
    public static final String SUFFIX_TEXT = "text.render.suffixText";  // String
    public static final String PADDING_LINE = "text.render.paddingLine";  // String
    public static final String TEXT_CONVERTER = "text.render.textConverter";    // TextConverter

    /**
     * Write chapter title before chapter text.
     */
    @Mapped(WRITE_TITLE)
    public boolean writeTitle = true;

    /**
     * Join chapter title chain with next specified separator.
     */
    @Mapped(JOIN_TITLES)
    public boolean joinTitles = false;

    /**
     * Separator for joining title chain.
     */
    @Mapped(TITLE_SEPARATOR)
    public String titleSeparator = " ";

    /**
     * Text added before chapter text and behind of chapter title.
     */
    @Mapped(PREFIX_TEXT)
    public String prefixText = null;

    /**
     * Write intro text before chapter text.
     */
    @Mapped(WRITE_INTRO)
    public boolean writeIntro = true;

    /**
     * Separator between intro text and chapter text.
     */
    @Mapped(INTRO_SEPARATOR)
    public String introSeparator = "-------";

    /**
     * Process lines in text (prepend paragraph prefix to line).
     */
    @Mapped(FORMAT_PARAGRAPH)
    public boolean formatParagraph = false;

    /**
     * Paragraph prefix used when formatParagraph is enable.
     */
    @Mapped(PARAGRAPH_PREFIX)
    public String paragraphPrefix = JEMessages.tr("text.render.paragraphPrefix");

    /**
     * Skip empty line, (enable when formatParagraph is enable).
     */
    @Mapped(SKIP_EMPTY_LINE)
    public boolean skipEmptyLine = true;

    /**
     * Line separator.
     */
    @Mapped(LINE_SEPARATOR)
    public String lineSeparator = System.getProperty("line.separator");

    /**
     * Text added at end of chapter text (before paddingLine),
     * append line separator to chapter text.
     */
    @Mapped(SUFFIX_TEXT)
    public String suffixText = null;

    /**
     * Add a addition line separator after chapter text.
     */
    @Mapped(PADDING_LINE)
    public boolean paddingLine = true;
}
