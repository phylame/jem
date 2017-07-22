/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

package jem.util.text.impl;

import jclp.io.IOUtils;
import jem.util.Variants;
import jem.util.flob.Flob;
import jem.util.text.AbstractText;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;

import static jclp.util.Validate.require;

public class FlobText extends AbstractText {
    static {
        Variants.mapClass(Variants.TEXT, Flob.class);
    }

    private final Flob flob;

    private final String encoding;

    public FlobText(String type, @NonNull Flob flob, String encoding) {
        super(type);
        this.flob = flob;
        this.encoding = encoding;
        require(encoding == null || Charset.isSupported(encoding), "unsupported encoding %s", encoding);
    }

    @Override
    @SneakyThrows(IOException.class)
    public String toString() {
        try (val input = flob.openStream()) {
            return IOUtils.toString(input, encoding);
        }
    }

    @Override
    @SneakyThrows(IOException.class)
    public Iterator<String> iterator() {
        try (val input = flob.openStream()) {
            return IOUtils.linesOf(input, encoding, false);
        }
    }

    @Override
    public long writeTo(@NonNull Writer writer) throws IOException {
        try (val reader = IOUtils.readerFor(flob.openStream(), encoding)) {
            return IOUtils.copy(reader, writer, -1);
        }
    }
}
