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

package jem.formats.umd;

import jclp.io.BufferedRandomAccessFile;
import jclp.io.PathUtils;
import jclp.io.ZLibUtils;
import jclp.log.Log;
import jclp.util.CollectionUtils;
import jclp.util.StringUtils;
import jem.Attributes;
import jem.Book;
import jem.Chapter;
import jem.epm.util.MakerException;
import jem.epm.util.NumberUtils;
import jem.epm.util.text.TextRender;
import jem.formats.util.M;
import jem.util.flob.Flob;
import lombok.NonNull;
import lombok.val;

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static jclp.io.ByteUtils.littleRender;

/**
 * <tt>Maker</tt> implement for UMD book.
 */
public class UmdMaker extends AbstractMaker<UmdOutConfig> {

    public UmdMaker() {
        super("umd", UmdOutConfig.class);
    }

    @Override
    public void make(@NonNull Book book, @NonNull OutputStream output, UmdOutConfig config)
            throws IOException, MakerException {
        if (config == null) {
            config = new UmdOutConfig();
        }
        val tuple = new Tuple(book, output, config);

        output.write(littleRender.putUInt32(UMD.MAGIC_NUMBER));
        switch (config.umdType) {
            case UMD.TEXT:
                makeText(tuple);
                break;
            case UMD.CARTOON:
                makeCartoon(tuple);
                break;
            case UMD.COMIC:
                makeComic(tuple);
                break;
            default:
                throw new MakerException(M.tr("umd.make.invalidType", config.umdType));
        }
    }

    private void makeText(Tuple tuple) throws IOException {
        writeUmdHead(UMD.TEXT, tuple);
        writeAttributes(tuple);

        // prepare text
        val cache = File.createTempFile("umd_", ".tmp");
        try (val raf = new BufferedRandomAccessFile(cache, "rw")) {
            val render = new UmdRender(this, raf);
            tuple.config.textConfig.lineSeparator = UMD.UMD_LINE_FEED;
            try {
                TextRender.renderBook(tuple.book, render, tuple.config.textConfig);
            } catch (Exception e) {
                throw new IOException(e);
            }

            val contentLength = raf.getFilePointer();
            raf.seek(0L);

            writeContentLength(contentLength, tuple);
            writeChapterOffsets(render.offsets, tuple);
            writeChapterTitles(render.titles, tuple);

            val checks = new LinkedList<Long>();
            writeText(raf, contentLength, checks, tuple);
            writeContentEnd(checks, tuple);

            writeCoverImage(tuple);
            writeSimplePageOffsets(contentLength, tuple);
            writeUmdEnd(tuple);
        } finally {
            if (!cache.delete()) {
                Log.e(getName(), "Failed delete UMD cached file: " + cache);
            }
        }
    }

    private void makeCartoon(Tuple tuple) throws IOException {
        writeUmdHead(UMD.CARTOON, tuple);
        writeAttributes(tuple);
        // ignored chapter offsets and titles
        List<Flob> images;
        String imageFormat = "jpg";

        // get cartoon images
        if (CollectionUtils.isNotEmpty(tuple.config.cartoonImages)) {
            images = tuple.config.cartoonImages;
            imageFormat = tuple.config.imageFormat;
        } else {
            images = new LinkedList<>();
            // prepare images
            for (val sub : tuple.book) {
                findImages(sub, images);
            }
        }

        writeChapterOffsets(null, tuple);
        writeChapterTitles(null, tuple);
        writeImageFormat(imageFormat, tuple);

        val checks = new LinkedList<Long>();
        writeImages(images, checks, tuple);
        writeContentEnd(checks, tuple);

        writeCoverImage(tuple);
        writeLicenseKey(tuple);
        writeUmdEnd(tuple);
    }

    private void makeComic(Tuple tuple) throws MakerException {
        throw new MakerException(M.tr("umd.make.unsupportedType", UMD.COMIC));
    }

    private void writeChunk(int id, boolean hasAddition, byte[] data, Tuple tuple) throws IOException {
        writeChunk(id, hasAddition ? UMD.CONTENT_APPENDED : UMD.CONTENT_SINGLE, data, tuple);
    }

    private void writeChunk(int id, int type, byte[] data, Tuple tuple) throws IOException {
        val output = tuple.output;
        output.write(UMD.CHUNK_SEPARATOR);
        output.write(littleRender.putUInt16(id));
        output.write(type);
        output.write(5 + data.length);
        output.write(data);
        tuple.writtenBytes += 5 + data.length;
    }

    private void writeAddition(long check, byte[] data, Tuple tuple) throws IOException {
        val output = tuple.output;
        output.write(UMD.ADDITION_SEPARATOR);
        output.write(littleRender.putUInt32(check));
        output.write(littleRender.putUInt32(9 + data.length));
        output.write(data);
        tuple.writtenBytes += 9 + data.length;
    }

    // 1
    private void writeUmdHead(int umdType, Tuple tuple) throws IOException {
        val data = new byte[3];
        data[0] = (byte) umdType;
        val rand = NumberUtils.randInteger(0x401, 0x7FFF);
        data[1] = (byte) ((rand & 0xFF00) >> 8);
        data[2] = (byte) (rand & 0xFF);
        writeChunk(UMD.CDT_UMD_HEAD, false, data, tuple);
    }

    private void writeMetaField(int id, String str, Tuple tuple) throws IOException {
        if (StringUtils.isEmpty(str)) {
            return;
        }
        writeChunk(id, false, str.getBytes(UMD.TEXT_ENCODING), tuple);
    }

    // 2-9
    private void writeAttributes(Tuple tuple) throws IOException {
        val book = tuple.book;
        writeMetaField(UMD.CDT_TITLE, Attributes.getTitle(book), tuple);
        writeMetaField(UMD.CDT_AUTHOR, Attributes.getAuthor(book), tuple);

        val calendar = Calendar.getInstance();
        Date date = Attributes.getPubdate(book);
        if (date == null) {
            date = Attributes.getDate(book);
        }
        if (date == null) {
            Log.i("UMD", "use current date");
            date = new Date();
        }
        calendar.setTime(date);
        writeMetaField(UMD.CDT_YEAR, Integer.toString(calendar.get(Calendar.YEAR)), tuple);
        writeMetaField(UMD.CDT_MONTH, Integer.toString(calendar.get(Calendar.MONTH) + 1), tuple);
        writeMetaField(UMD.CDT_DAY, Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)), tuple);

        writeMetaField(UMD.CDT_GENRE, Attributes.getGenre(book), tuple);
        writeMetaField(UMD.CDT_PUBLISHER, Attributes.getPublisher(book), tuple);
        writeMetaField(UMD.CDT_VENDOR, Attributes.getVendor(book), tuple);
    }

    // B
    private void writeContentLength(long length, Tuple tuple) throws IOException {
        writeChunk(UMD.CDT_CONTENT_LENGTH, false, littleRender.putUInt32(length), tuple);
    }

    // 83
    private void writeChapterOffsets(List<Long> offsets, Tuple tuple) throws IOException {
        val rand = NumberUtils.randInteger(0x3000, 0x3FFF);
        writeChunk(UMD.CDT_CHAPTER_OFFSET, true, littleRender.putUInt32(rand), tuple);
        byte[] data;
        if (CollectionUtils.isNotEmpty(offsets)) {
            val out = new ByteArrayOutputStream();
            for (val offset : offsets) {
                out.write(littleRender.putUInt32(offset));
            }
            data = out.toByteArray();
        } else {
            data = new byte[0];
        }
        writeAddition(rand, data, tuple);
    }

    // 84
    private void writeChapterTitles(List<String> titles, Tuple tuple) throws IOException {
        val rand = NumberUtils.randInteger(0x4000, 0x4FFF);
        writeChunk(UMD.CDT_CHAPTER_TITLE, true, littleRender.putUInt32(rand), tuple);
        byte[] data;
        if (CollectionUtils.isNotEmpty(titles)) {
            val out = new ByteArrayOutputStream();
            byte[] bytes;
            for (val title : titles) {
                bytes = title.getBytes(UMD.TEXT_ENCODING);
                out.write(bytes.length);
                out.write(bytes);
            }
            data = out.toByteArray();
        } else {
            data = new byte[0];
        }
        writeAddition(rand, data, tuple);
    }

    // E
    private void writeImageFormat(String format, Tuple tuple) throws IOException {
        writeChunk(UMD.CDT_IMAGE_FORMAT, false, new byte[]{(byte) UMD.formatOfName(format)}, tuple);
    }

    // F1
    private void writeLicenseKey(Tuple tuple) throws IOException {
        byte[] data;
        val key = tuple.book.getAttributes().get("license_key", (Object) null);
        if (key != null && key instanceof byte[]) {
            data = (byte[]) key;
        } else {
            data = new byte[]{ // 0 x 16
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0
            };
        }
        writeChunk(UMD.CDT_LICENSE_KEY, false, data, tuple);
    }

    // A
    private void writeContentId(Tuple tuple) throws IOException {
        val id = tuple.book.getAttributes().get("book_id", (Object) null);
        int bookId = id != null && id instanceof Integer ? (Integer) id : NumberUtils.randInteger(0, 1000) + 0x10000000;
        writeChunk(UMD.CDT_CONTENT_ID, false, littleRender.putUInt32(bookId), tuple);
    }

    // 81
    private void writeContentEnd(List<Long> checks, Tuple tuple) throws IOException {
        val rand = NumberUtils.randInteger(0x2000, 0x2FFF);
        writeChunk(UMD.CDT_CONTENT_END, true, littleRender.putUInt32(rand), tuple);
        val out = new ByteArrayOutputStream();
        for (val check : checks) {
            out.write(littleRender.putUInt32(check));
        }
        writeAddition(rand, out.toByteArray(), tuple);
    }

    // 82
    private void writeCoverImage(Tuple tuple) throws IOException {
        val cover = Attributes.getCover(tuple.book);
        if (cover == null) {
            return;
        }
        val type = UMD.formatOfName(PathUtils.extName(cover.getName()));
        val rand = NumberUtils.randInteger(0x1000, 0x1FFF);
        val data = new byte[]{(byte) type, 0, 0, 0, 0};
        System.arraycopy(littleRender.putUInt32(rand), 0, data, 1, 4);
        writeChunk(UMD.CDT_COVER_IMAGE, true, data, tuple);
        writeAddition(rand, cover.readAll(), tuple);
    }

    // 87, placeholder page
    private void writeSimplePageOffsets(long contentLength, Tuple tuple) throws IOException {
        int[][] pages = {
                {0x1, 0x10, 0xD0},
                {0x1, 0x10, 0xB0},
                {0x1, 0x0C, 0xD0},
                {0x1, 0x10, 0xB0},
                {0x5, 0x0A, 0xA6}
        };
        val buf6 = new byte[6];
        val buf12 = new byte[12];
        long rand;
        for (val page : pages) {
            buf6[0] = (byte) page[1];
            buf6[1] = (byte) page[2];
            rand = NumberUtils.randLong(0x7000, 0x7FFF);
            System.arraycopy(littleRender.putUInt32(rand), 0, buf6, 2, 4);
            writeChunk(UMD.CDT_PAGE_OFFSET, page[0], buf6, tuple);

            System.arraycopy(littleRender.putUInt32(17), 0, buf12, 0, 4);
            System.arraycopy(littleRender.putUInt32(0), 0, buf12, 4, 4);
            System.arraycopy(littleRender.putUInt32(contentLength), 0, buf12, 8, 4);
            writeAddition(rand, buf12, tuple);
        }
    }

    // C
    private void writeUmdEnd(Tuple tuple) throws IOException {
        long length = tuple.writtenBytes;
        length += 1 + 2 + 2 + 4 + 4;
        writeChunk(UMD.CDT_UMD_END, false, littleRender.putUInt32(length), tuple);
    }

    private void writeText(BufferedRandomAccessFile file, long contentLength, List<Long> checks, Tuple tuple)
            throws IOException {
        int count = (int) (contentLength >> 15); // div 0x8000
        count += ((contentLength & 0x7FFF) > 0) ? 1 : 0; // mod 0x8000 > 0
        val randValA = NumberUtils.randInteger(0, count);
        val randValB = NumberUtils.randInteger(0, count);
        for (int i = 0; i < count; ++i) {
            long checkVal = NumberUtils.randLong(4026530000L, 4294970000L);
            checks.add(checkVal);
            byte[] buf = new byte[UMD.BLOCK_SIZE];
            file.read(buf);
            writeAddition(checkVal, ZLibUtils.compress(buf), tuple);
            if (i == randValA) {
                writeLicenseKey(tuple);
            } else if (i == randValB) {
                writeContentId(tuple);
            }
        }
    }

    void writeString(RandomAccessFile file, String text) throws IOException {
        file.write(text.getBytes(UMD.TEXT_ENCODING));
    }

    private void findImages(Chapter chapter, List<Flob> images) {
        val cover = Attributes.getCover(chapter);
        if (cover != null) {
            images.add(cover);
        }
        for (val sub : chapter) {
            findImages(sub, images);
        }
    }

    private void writeImages(List<Flob> images, LinkedList<Long> checks, Tuple tuple) throws IOException {
        if (images.isEmpty()) {
            return;
        }
        val rand = NumberUtils.randInteger(0, images.size() - 1);
        int i = 0;
        for (val img : images) {
            long checkVal = NumberUtils.randLong(4026530000L, 4294970000L);
            checks.add(checkVal);
            writeAddition(checkVal, img.readAll(), tuple);
            if (i++ == rand) {
                writeContentId(tuple);
            }
        }
    }

    private class Tuple {
        private Book book;
        private OutputStream output;
        private UmdOutConfig config;
        private long writtenBytes = 0L;

        private Tuple(Book book, OutputStream output, UmdOutConfig config) {
            this.book = book;
            this.output = output;
            this.config = config;
        }
    }
}
