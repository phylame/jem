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

package pw.phylame.jem.util;

import lombok.NonNull;
import lombok.SneakyThrows;
import pw.phylame.ycl.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
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
        return forString("", type);
    }

    private static class RawText extends AbstractText {
        static {
            Variants.mapType(RawText.class, Variants.TEXT);
        }

        private final CharSequence text;

        private RawText(CharSequence cs, String type) {
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

        private FlobText(Flob file, String encoding, String type) {
            super(type);
            this.file = file;
            this.encoding = encoding;
        }

        @SneakyThrows(IOException.class)
        @Override
        public String getText() {
            try (InputStream stream = file.openStream()) {
                return IOUtils.toString(stream, encoding);

            }
        }

        @SneakyThrows(IOException.class)
        @Override
        public List<String> getLines(boolean skipEmpty) {
            try (InputStream stream = file.openStream()) {
                return IOUtils.toLines(stream, encoding, skipEmpty);
            }
        }

        @Override
        public int writeTo(Writer writer) throws IOException {
            try (Reader reader = IOUtils.openReader(file.openStream(), encoding)) {
                return IOUtils.copy(reader, writer, -1);
            }
        }
    }
}
