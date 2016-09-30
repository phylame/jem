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

package pw.phylame.jem.epm.util.text;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.util.text.Text;
import pw.phylame.jem.util.text.Texts;
import pw.phylame.ycl.util.CollectUtils;

import java.util.LinkedList;

import static pw.phylame.ycl.util.StringUtils.*;

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
        for (val sub : book) {
            walkChapter(sub, maker);
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
    public static int renderLines(@NonNull Text text, @NonNull TextWriter writer, @NonNull TextConfig config)
            throws Exception {
        return renderLines(text, writer, config, false);
    }

    private static int renderLines(Text text, TextWriter writer, TextConfig config, boolean prependNL) throws Exception {
        val lines = text.getLines(config.skipEmptyLine);
        if (CollectUtils.isEmpty(lines)) {
            return 0;
        }
        int ix = 1, size = lines.size();
        if (prependNL && size > 0) {
            writer.writeText(config.lineSeparator);
        }
        for (String line : lines) {
            line = trimmed(line);
            writer.writeText(config.paragraphPrefix + line);
            if (ix++ != size) {
                writer.writeText(config.lineSeparator);
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

    private static boolean renderText(Text text, TextWriter writer, TextConfig config,
                                      boolean prependLF) throws Exception {
        if (config.formatParagraph) {
            return renderLines(text, writer, config, prependLF) > 0;
        } else {
            val str = text.getText();
            if (!str.isEmpty()) {
                if (prependLF) {
                    writer.writeText(config.lineSeparator);
                }
                writer.writeText(str);
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
        public void startChapter(String title) throws Exception {
        }

        @Override
        public void writeText(String text) throws Exception {
            b.append(text);
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

        private LinkedList<String> titleStack;

        private RenderHelper(TextWriter writer, TextConfig config) {
            this.config = config;
            this.writer = writer;
            if (config.joinTitles) {
                titleStack = new LinkedList<>();
            }
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
            writer.startChapter(title);

            // title
            boolean writtenTitle = false;
            if (config.writeTitle) {
                writer.writeText(title);
                writtenTitle = true;
            }
            // prefix
            if (isNotEmpty(config.prefixText)) {
                writer.writeText(writtenTitle ? lineSeparator + config.prefixText : config.prefixText);
            }
            // intro
            if (config.writeIntro) {
                val intro = Attributes.getIntro(chapter);
                if (intro != null && renderText(intro, writer, config, true)) {
                    writer.writeText(lineSeparator + config.introSeparator);
                }
            }
            // text
            val text = chapter.getText();
            renderText(text != null ? text : Texts.forEmpty(Text.PLAIN), writer, config, true);
            // suffix
            if (isNotEmpty(config.suffixText)) {
                writer.writeText(lineSeparator + config.suffixText);
            }
            // padding line
            if (config.paddingLine) {
                writer.writeText(lineSeparator);
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
