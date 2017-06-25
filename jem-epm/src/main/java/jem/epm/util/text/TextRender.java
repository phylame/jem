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

package jem.epm.util.text;

import jem.Attributes;
import jem.Chapter;
import jem.util.text.Text;
import jem.util.text.Texts;
import lombok.NonNull;
import lombok.val;
import jclp.util.CollectionUtils;

import java.util.LinkedList;

import static jclp.util.StringUtils.*;

/**
 * Render book text with plain style.
 */
public final class TextRender {
    private TextRender() {
    }

    /**
     * Renders chapter of book to contents with one level.
     */
    public static void renderBook(@NonNull Chapter book, @NonNull TextWriter writer, @NonNull TextConfig config)
            throws Exception {
        val maker = new RenderHelper(writer, config);
        for (val chapter : book) {
            walkChapter(chapter, maker);
        }
    }

    /**
     * Renders lines of text in <tt>Text</tt> to specified writer.
     *
     * @param text   the text source
     * @param writer the destination writer
     * @param config render config
     * @return number of written lines
     * @throws Exception if occurs error while rendering text
     */
    public static int renderLines(Text text, TextWriter writer, TextConfig config) throws Exception {
        return renderLines(text, writer, config, false);
    }

    private static int renderLines(Text text, TextWriter writer, TextConfig config, boolean prependNL)
            throws Exception {
        val lines = text.getLines(config.skipEmptyLine);
        if (CollectionUtils.isEmpty(lines)) {
            return 0;
        }
        int ix = 1, size = lines.size();
        if (prependNL && size > 0) {
            writer.write(config.lineSeparator);
        }
        for (val line : lines) {
            writer.write(config.paragraphPrefix).write(trimmed(line));
            if (ix++ != size) {
                writer.write(config.lineSeparator);
            }
        }
        return size;
    }

    public static String renderLines(@NonNull Text text, @NonNull TextConfig config) throws Exception {
        val writer = new StringWriter();
        renderLines(text, writer, config);
        return writer.toString();
    }

    /**
     * Renders text in <tt>Text</tt> to specified writer.
     *
     * @param text   the text source
     * @param writer the destination writer
     * @param config render config
     * @return written state, <tt>true</tt> if has text written, otherwise not
     * @throws Exception if occurs error while rendering text
     */
    public static boolean renderText(@NonNull Text text, @NonNull TextWriter writer, @NonNull TextConfig config)
            throws Exception {
        return renderText(text, writer, config, false);
    }

    private static boolean renderText(Text text, TextWriter writer, TextConfig config, boolean prependLF)
            throws Exception {
        if (config.formatParagraph) {
            return renderLines(text, writer, config, prependLF) > 0;
        } else {
            val str = text.getText();
            if (!str.isEmpty()) {
                if (prependLF) {
                    writer.write(config.lineSeparator);
                }
                writer.write(str);
                return true;
            } else {
                return false;
            }
        }
    }

    public static String renderText(@NonNull Text text, @NonNull TextConfig config) throws Exception {
        return config.formatParagraph ? renderLines(text, config) : text.getText();
    }

    private static class StringWriter implements TextWriter {
        private final StringBuilder b = new StringBuilder();

        @Override
        public String toString() {
            return b.toString();
        }

        @Override
        public void beginChapter(String title) throws Exception {
        }

        @Override
        public StringWriter write(String text) throws Exception {
            b.append(text);
            return this;
        }

        @Override
        public void endChapter() throws Exception {
        }
    }

    private static void walkChapter(Chapter chapter, RenderHelper maker) throws Exception {
        maker.beginItem(chapter);

        maker.writeText(chapter);

        for (val sub : chapter) {
            walkChapter(sub, maker);
        }

        maker.endItem();
    }

    private static class RenderHelper {
        private final TextWriter writer;
        private final TextConfig config;

        private final LinkedList<String> titleStack;

        private RenderHelper(TextWriter writer, TextConfig config) {
            this.config = config;
            this.writer = writer;
            titleStack = config.joinTitles ? new LinkedList<String>() : null;
        }

        private void beginItem(Chapter chapter) {
            if (config.joinTitles) {
                titleStack.addLast(Attributes.getTitle(chapter));
            }
        }

        private void writeText(Chapter chapter) throws Exception {
            val lineSeparator = config.lineSeparator;
            val title = config.joinTitles
                    ? join(config.titleSeparator, titleStack)
                    : Attributes.getTitle(chapter);
            writer.beginChapter(title);

            // title
            boolean titleWritten = false;
            if (config.writeTitle) {
                writer.write(title);
                titleWritten = true;
            }
            // prefix
            if (isNotEmpty(config.prefixText)) {
                writer.write(titleWritten ? lineSeparator + config.prefixText : config.prefixText);
            }
            // intro
            if (config.writeIntro) {
                val intro = Attributes.getIntro(chapter);
                if (intro != null && renderText(intro, writer, config, true)) {
                    writer.write(lineSeparator + config.introSeparator);
                }
            }
            // text
            val text = chapter.getText();
            renderText(text != null ? text : Texts.forEmpty(Texts.PLAIN), writer, config, true);
            // suffix
            if (isNotEmpty(config.suffixText)) {
                writer.write(lineSeparator + config.suffixText);
            }
            // padding line
            if (config.paddingLine) {
                writer.write(lineSeparator);
            }
            writer.endChapter();
        }

        private void endItem() {
            if (config.joinTitles) {
                titleStack.removeLast();
            }
        }
    }
}
