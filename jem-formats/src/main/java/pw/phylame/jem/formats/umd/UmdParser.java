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

package pw.phylame.jem.formats.umd;

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
import pw.phylame.jem.formats.util.M;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.jem.util.text.AbstractText;
import pw.phylame.ycl.io.PathUtils;
import pw.phylame.ycl.io.ZLibUtils;
import pw.phylame.ycl.util.CollectionUtils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * <tt>Parser</tt> implement for UMD book.
 */
public class UmdParser extends BinaryParser<NonConfig> {
    public UmdParser() {
        super("umd", null);
    }

    @Override
    public Book parse(@NonNull RandomAccessFile input, NonConfig config) throws IOException, ParserException {
        return parse(input);
    }

    @Override
    protected void onBadInput() throws ParserException {
        throw new ParserException(M.tr("umd.parse.invalidFile"));
    }

    @Override
    protected void onBadInput(String key, Object... args) throws ParserException {
        throw new ParserException(M.tr(key, args));
    }

    public Book parse(@NonNull RandomAccessFile file) throws IOException, ParserException {
        if (readUInt32(file) != UMD.MAGIC_NUMBER) {
            onBadInput("umd.parse.invalidMagic");
        }
        val tuple = new Tuple(file);
        int ch;
        while ((ch = file.read()) != -1) {
            switch (ch) {
                case UMD.CHUNK_SEPARATOR:
                    readChunk(tuple);
                    break;
                case UMD.ADDITION_SEPARATOR:
                    readContent(tuple);
                    break;
                default:
                    onBadInput("umd.parse.badSeparator", ch);
            }
        }
        return tuple.book;
    }

    private void readChunk(Tuple tuple) throws IOException, ParserException {
        val file = tuple.file;
        val book = tuple.book;

        val chunkId = readUInt16(file);
        file.skipBytes(1);
        val length = file.read() - 5;

        switch (chunkId) {
            case UMD.CDT_UMD_HEAD: {
                val umdType = file.read();
                if (umdType != UMD.TEXT && umdType != UMD.CARTOON) {
                    onBadInput("umd.parse.invalidType", umdType);
                }
                tuple.umdType = umdType;
                file.skipBytes(2);
            }
            break;
            case UMD.CDT_TITLE: {
                Attributes.setTitle(book, readString(file, length));
            }
            break;
            case UMD.CDT_AUTHOR: {
                Attributes.setAuthor(book, readString(file, length));
            }
            break;
            case UMD.CDT_YEAR: {
                tuple.year = Integer.parseInt(readString(file, length));
            }
            break;
            case UMD.CDT_MONTH: {
                tuple.month = Integer.parseInt(readString(file, length)) - 1;
            }
            break;
            case UMD.CDT_DAY: {
                tuple.day = Integer.parseInt(readString(file, length));
            }
            break;
            case UMD.CDT_GENRE: {
                Attributes.setGenre(book, readString(file, length));
            }
            break;
            case UMD.CDT_PUBLISHER: {
                Attributes.setPublisher(book, readString(file, length));
            }
            break;
            case UMD.CDT_VENDOR: {
                Attributes.setVendor(book, readString(file, length));
            }
            break;
            case UMD.CDT_CONTENT_LENGTH: {
                tuple.contentLength = readUInt32(file);
            }
            break;
            case UMD.CDT_CHAPTER_OFFSET: {
                file.skipBytes(9);
                readChapterOffsets(tuple);
            }
            break;
            case UMD.CDT_CHAPTER_TITLE: {
                file.skipBytes(9);
                readChapterTitles(tuple);
            }
            break;
            case UMD.CDT_CONTENT_END: {
                file.skipBytes(9);
                readContentEnd(tuple.file);
            }
            break;
            case 0x85:
            case 0x86: {
                file.skipBytes(9);
                skipBlock(tuple.file);
            }
            break;
            case UMD.CDT_IMAGE_FORMAT: {
                tuple.imageFormat = file.read();
            }
            break;
            case UMD.CDT_CONTENT_ID: {
                book.getAttributes().put("book_id", readUInt32(file));
            }
            break;
            case UMD.CDT_CDS_KEY: {
                val bytes = readData(file, length, "umd.parse.badCDSKey");
                book.getAttributes().put("cds_key", Flobs.forBytes("cds_key", bytes, PathUtils.UNKNOWN_MIME));
            }
            break;
            case UMD.CDT_LICENSE_KEY: {
                val bytes = readData(file, length, "umd.parse.badLicenseKey");
                book.getAttributes().put("license_key", Flobs.forBytes("license_key", bytes, PathUtils.UNKNOWN_MIME));
            }
            break;
            case UMD.CDT_COVER_IMAGE: {
                tuple.coverFormat = file.read();
                file.skipBytes(9);
                readCoverImage(tuple);
            }
            break;
            case UMD.CDT_PAGE_OFFSET: {
                // ignore page information
                file.skipBytes(11);
                readPageOffsets(tuple);
            }
            break;
            case UMD.CDT_UMD_END: {
                if (readUInt32(file) != file.getFilePointer()) {
                    onBadInput("umd.parse.badEnd");
                }
                if (tuple.year > 0 && tuple.day > 0) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.YEAR, tuple.year);
                    calendar.set(Calendar.MONTH, tuple.month);
                    calendar.set(Calendar.DAY_OF_MONTH, tuple.day);
                    Attributes.setPubdate(book, calendar.getTime());
                }
            }
            break;
            case 0xD:
                // ignored
                break;
        }
    }

    private String readString(RandomAccessFile file, int length) throws IOException, ParserException {
        return new String(readData(file, length), UMD.TEXT_ENCODING);
    }

    private void skipBlock(RandomAccessFile file) throws IOException, ParserException {
        file.skipBytes((int) readUInt32(file) - 9);
    }

    private void readChapterOffsets(Tuple tuple) throws IOException, ParserException {
        val file = tuple.file;
        val book = tuple.book;
        tuple.chapterCount = (int) ((readUInt32(file) - 9) >> 2); // div 4

        if (tuple.chapterCount == 0) {     // no chapter
            return;
        }

        long prevOffset = readUInt32(file);
        UmdText text = new UmdText(file, prevOffset, 0, tuple.blocks);
        book.append(new Chapter("", text));
        for (int ix = 1; ix < tuple.chapterCount; ++ix) {
            long offset = readUInt32(file);
            text.size = offset - prevOffset;
            text = new UmdText(file, offset, 0, tuple.blocks);
            prevOffset = offset;
            book.append(new Chapter("", text));
        }
        text.size = tuple.contentLength - prevOffset;
    }

    private void readChapterTitles(Tuple data) throws IOException, ParserException {
        val file = data.file;
        file.skipBytes(4);
        for (val sub : data.book) {
            Attributes.setTitle(sub, readString(file, file.read()));
        }
    }

    private void readContentEnd(RandomAccessFile file) throws IOException, ParserException {
        // ignored
        skipBlock(file);
    }

    private void readCoverImage(Tuple data) throws IOException, ParserException {
        val file = data.file;
        val length = readUInt32(file) - 9;
        val format = UMD.nameOfFormat(data.coverFormat);
        Attributes.setCover(data.book, Flobs.forBlock("cover." + format, file, file.getFilePointer(), length, "image/" + format));
        file.skipBytes((int) length);
    }

    private void readPageOffsets(Tuple data) throws IOException, ParserException {
        // ignored
        skipBlock(data.file);
    }

    private void readContent(Tuple data) throws IOException, ParserException {
        val file = data.file;
        val book = data.book;

        file.skipBytes(4);
        val length = readUInt32(file) - 9;
        val offset = file.getFilePointer();

        switch (data.umdType) {
            case UMD.TEXT: {
                data.blocks.add(new TextBlock((int) file.getFilePointer(), (int) length));
            }
            break;
            case UMD.CARTOON: {
                val format = UMD.nameOfFormat(data.imageFormat);
                val name = String.format("img_%d.%s", book.size() + 1, format);
                val chapter = new Chapter(String.valueOf(book.size() + 1));
                Attributes.setCover(chapter, Flobs.forBlock(name, file, offset, length, "image/" + format));
                book.append(chapter);
            }
            break;
            case UMD.COMIC:
                break;
        }
        file.skipBytes((int) length);
    }


    @AllArgsConstructor
    private class TextBlock {
        private final int offset, length;
    }

    private class Tuple {
        private final RandomAccessFile file;
        private final Book book;
        private int umdType;

        private int year = 0, month = 0, day = 0;

        private int chapterCount;

        private long contentLength;
        private int coverFormat, imageFormat;

        private final List<TextBlock> blocks = new ArrayList<>();

        private Tuple(RandomAccessFile file) {
            this.file = file;
            book = new Book();
        }
    }

    private class UmdText extends AbstractText {
        private final RandomAccessFile file;
        private final long offset;
        private long size;
        private final List<TextBlock> blocks;

        private UmdText(RandomAccessFile file, long offset, long size, List<TextBlock> blocks) {
            super(PLAIN);
            this.file = file;
            this.offset = offset;
            this.size = size;
            this.blocks = blocks;
        }

        private String rawText() throws IOException {
            int index = (int) (offset >> 15);   // div 0x8000
            val start = (int) (offset & 0x7FFF);    // mod 0x8000
            int length = -start;
            val b = new StringBuilder();
            do {
                val block = blocks.get(index++);
                file.seek(block.offset);
                byte[] bytes = new byte[block.length];
                bytes = ZLibUtils.decompress(bytes, 0, file.read(bytes));
                length += bytes.length;
                b.append(new String(bytes, UMD.TEXT_ENCODING));
                if (size <= length) {
                    return b.substring(start >> 1, (int) (start + size) >> 1); // div 2
                }
            } while (true);
        }

        @SneakyThrows(IOException.class)
        @Override
        public String getText() {
            return rawText().replaceAll(UMD.UMD_LINE_FEED, System.lineSeparator());
        }

        @SneakyThrows(IOException.class)
        @Override
        public List<String> getLines(boolean skipEmpty) {
            return CollectionUtils.listOf(rawText().split(UMD.UMD_LINE_FEED));
        }
    }
}

