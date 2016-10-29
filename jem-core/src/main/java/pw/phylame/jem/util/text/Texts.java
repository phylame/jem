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

package pw.phylame.jem.util.text;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import pw.phylame.jem.util.Variants;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.util.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

/**
 * Factory class for creating <code>Text</code> instance.
 */
public final class Texts {
    private Texts() {
    }

    public static Text forString(@NonNull CharSequence str, String type) {
        return new RawText(str, type);
    }

    public static Text forFile(@NonNull Flob file, String encoding, String type) {
        return new FlobText(file, encoding, type);
    }

    public static Text forEmpty(String type) {
        return forString(StringUtils.EMPTY_TEXT, type);
    }

    private static class RawText extends AbstractText {
        static {
            Variants.mapType(RawText.class, Variants.TEXT);
        }

        private final CharSequence text;

        private RawText(@NonNull CharSequence cs, String type) {
            super(type);
            this.text = cs;
        }

        @Override
        public String getText() {
            return text.toString();
        }
    }

    private static class FlobText extends AbstractText {
        static {
            Variants.mapType(FlobText.class, Variants.TEXT);
        }

        private final Flob file;
        private final String encoding;

        private FlobText(@NonNull Flob file, String encoding, String type) {
            super(type);
            this.file = file;
            this.encoding = encoding;
        }

        @SneakyThrows(IOException.class)
        @Override
        public String getText() {
            try (val in = file.openStream()) {
                return IOUtils.toString(in, encoding);
            }
        }

        @Override
        @SneakyThrows(IOException.class)
        public List<String> getLines(boolean skipEmpty) {
            try (val in = file.openStream()) {
                return IOUtils.toLines(in, encoding, skipEmpty);
            }
        }

        @Override
        @SneakyThrows(IOException.class)
        public Iterator<String> iterator() {
            try (val reader = IOUtils.readerFor(file.openStream(), encoding)) {
                return IOUtils.linesOf(reader, false);
            }
        }

        @Override
        public long writeTo(@NonNull Writer writer) throws IOException {
            try (val reader = IOUtils.readerFor(file.openStream(), encoding)) {
                return IOUtils.copy(reader, writer, -1);
            }
        }
    }
}
