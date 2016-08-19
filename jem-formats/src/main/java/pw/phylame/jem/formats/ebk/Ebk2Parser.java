/*
 * Copyright 2014-2015 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.formats.ebk;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.epm.base.BinaryParser;
import pw.phylame.jem.epm.util.ParserException;
import pw.phylame.jem.epm.util.config.NonConfig;
import pw.phylame.jem.formats.util.JFMessages;
import pw.phylame.jem.util.text.AbstractText;
import pw.phylame.ycl.io.ByteUtils;
import pw.phylame.ycl.io.ZLibUtils;
import pw.phylame.ycl.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class Ebk2Parser extends BinaryParser<NonConfig> {
    public Ebk2Parser() {
        super("ebk", null);
    }

    @Override
    protected void onBadInput() throws ParserException {
        throw new ParserException(JFMessages.tr("ebk.parse.invalidFile"));
    }

    @Override
    protected void onBadInput(String key, Object... args) throws ParserException {
        throw new ParserException(JFMessages.tr(key, args));
    }

    @Override
    public Book parse(@NonNull RandomAccessFile input, NonConfig config) throws IOException, ParserException {
        val tuple = new Tuple(input);
        readHeader(tuple);
        readIndexes(tuple);
        return tuple.book;
    }

    private void readHeader(Tuple tuple) throws IOException, ParserException {
        val file = tuple.file;
        val book = tuple.book;

        book.getAttributes().put("book_id", readUInt32(file));
        tuple.headerSize = readUInt16(file);
        int version = readUInt16(file);
        if (version != 2) {
            onBadInput("ebk.parse.unsupportedVersion", version);
        }

        file.skipBytes(4);     // ebk2 size
        Attributes.setTitle(book, readString(file, 64));
        file.skipBytes(4);     // file size
        tuple.indexesSize = readUInt32(file);
        file.skipBytes(4);     // first block
        tuple.chapterCount = readUInt16(file);
        tuple.blockCount = readUInt16(file);
        tuple.mediaCount = (int) readUInt32(file);
        file.skipBytes(8);     // media size and txt size
    }

    private void readIndexes(Tuple data) throws IOException, ParserException {
        val in = new ByteArrayInputStream(ZLibUtils.decompress(readData(data.file, (int) data.indexesSize)));
        readChapters(data, in);
        readBlocks(data, in);
    }

    private void readChapters(Tuple data, InputStream stream) throws IOException, ParserException {
        String title;
        EbkText text;
        long offset, length;

        for (int i = 0; i < data.chapterCount; ++i) {
            title = readString(stream, 64);
            offset = readUInt32(stream);
            length = readUInt32(stream);
            text = new EbkText(data.file, data.blocks, offset, length);
            text.headSize = data.headerSize;
            text.indexSize = data.indexesSize;
            data.book.append(new Chapter(title, text));
        }
    }

    private void readBlocks(Tuple data, InputStream stream) throws IOException, ParserException {
        long offset, length;
        for (int i = 0; i < data.blockCount; ++i) {
            offset = readUInt32(stream);
            length = readUInt32(stream);
            data.blocks.add(new TextBlock(offset, length));
        }
    }

    private byte[] readBytes(InputStream in, long size) throws IOException, ParserException {
        byte[] b = new byte[(int) size];
        if (in.read(b) != size) {
            onBadInput("ebk.parse.invalidFile");
        }
        return b;
    }

    private long readUInt32(InputStream in) throws IOException, ParserException {
        return ByteUtils.littleParser.getInt32(readBytes(in, 4), 0);
    }

    private String readString(RandomAccessFile file, int length) throws IOException, ParserException {
        return StringUtils.trimmed(new String(readData(file, length), EBK.TEXT_ENCODING));
    }

    private String readString(InputStream in, int length) throws IOException, ParserException {
        return StringUtils.trimmed(new String(readBytes(in, length), EBK.TEXT_ENCODING));
    }

    @AllArgsConstructor
    private class TextBlock {
        private final long offset, size;
    }

    private class Tuple {
        private final RandomAccessFile file;
        private final Book book;

        private int headerSize;
        private long indexesSize;

        private int chapterCount;
        private int blockCount;
        private int mediaCount;

        private final List<TextBlock> blocks = new ArrayList<>();

        private Tuple(RandomAccessFile file) {
            this.file = file;
            book = new Book();
        }
    }

    private class EbkText extends AbstractText {
        private final RandomAccessFile file;
        private final List<TextBlock> blocks;
        private final long offset;
        private final long size;

        private int headSize;
        private long indexSize;

        private EbkText(RandomAccessFile file, List<TextBlock> blocks, long offset, long size) {
            super(PLAIN);
            this.file = file;
            this.blocks = blocks;
            this.offset = offset;
            this.size = size;
        }

        private String rawText() throws IOException {
            int index = (int) (offset >> 16);   // div 0x10000
            val start = (int) (offset & 0x9999);  // mod 0x10000
            int length = -start;
            val b = new StringBuilder();
            do {
                val block = blocks.get(index++);
                file.seek(headSize + indexSize + block.offset);
                byte[] bytes = new byte[(int) block.size];
                bytes = ZLibUtils.decompress(bytes, 0, file.read(bytes));
                length += bytes.length;
                b.append(new String(bytes, EBK.TEXT_ENCODING));
                if (size <= length) {
                    return b.substring(start >> 1, (int) (start + size) >> 1);
                }
            } while (true);
        }

        @SneakyThrows(IOException.class)
        @Override
        public String getText() {
            return rawText();
        }
    }
}
