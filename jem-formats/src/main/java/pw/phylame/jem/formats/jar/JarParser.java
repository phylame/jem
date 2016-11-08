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

package pw.phylame.jem.formats.jar;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.ZipFile;

import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.epm.base.ZipParser;
import pw.phylame.jem.epm.util.ParserException;
import pw.phylame.jem.epm.util.ZipUtils;
import pw.phylame.jem.formats.util.M;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.jem.util.text.Texts;

/**
 * <tt>Parser</tt> implement for JAR book.
 */
public class JarParser extends ZipParser<JarInConfig> {
    public JarParser() {
        super("jar", null);
    }

    @Override
    public Book parse(ZipFile zip, JarInConfig config) throws IOException, ParserException {
        return parse(zip);
    }

    public Book parse(ZipFile zip) throws IOException, ParserException {
        val book = new Book();
        parseMetadata(book, zip);
        return book;
    }

    private void parseMetadata(Book book, ZipFile zip) throws IOException, ParserException {
        try (val in = new BufferedInputStream(ZipUtils.openStream(zip, "0"))) {
            val input = new DataInputStream(in);
            if (input.readInt() != JAR.MAGIC_NUMBER) {
                throw new ParserException(M.tr("jar.parse.badMetadata", zip.getName()));
            }
            Attributes.setTitle(book, readString(input, 1));

            val count = Integer.parseInt(readString(input, 2));
            for (int i = 0; i < count; ++i) {
                val items = readString(input, 2).split(",");
                if (items.length < 3) {
                    throw new ParserException(M.tr("jar.parse.badMetadata", zip.getName()));
                }
                val flob = Flobs.forZip(zip, items[0], "text/plain");
                book.append(new Chapter(items[2], Texts.forFlob(flob, JAR.TEXT_ENCODING, Texts.PLAIN)));
            }

            input.skipBytes(2); // what ?
            val str = readString(input, 2);
            if (!str.isEmpty()) {
                Attributes.setIntro(book, Texts.forString(str, Texts.PLAIN));
            }
        }
    }

    private String readString(DataInput input, int size) throws IOException {
        val length = (size == 1) ? input.readByte() : input.readShort();
        byte[] b = new byte[length];
        input.readFully(b);
        return new String(b, JAR.METADATA_ENCODING);
    }
}
