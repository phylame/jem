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

package jem.formats.mobi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import jem.Attributes;
import jem.Book;
import jem.epm.impl.BinaryParser;
import jem.epm.util.ParserException;
import jem.util.flob.Flob;
import jem.util.flob.FlobWrapper;
import jem.util.flob.Flobs;
import jem.util.flob.Flobs.BlockFlob;
import jem.util.text.Texts;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import pw.phylame.commons.io.ByteUtils;
import pw.phylame.commons.io.IOUtils;
import pw.phylame.commons.io.Lz77Utils;
import pw.phylame.commons.log.Log;
import pw.phylame.commons.util.DateUtils;
import pw.phylame.commons.util.MiscUtils;
import pw.phylame.commons.util.StringUtils;

public class MobiParser extends BinaryParser<MobiInConfig> {
    private static final String TAG = MobiParser.class.getSimpleName();

    public MobiParser() {
        super("mobi", MobiInConfig.class);
    }

    @Override
    protected Book parse(RandomAccessFile file, MobiInConfig config) throws IOException, ParserException {
        if (config == null) {
            config = new MobiInConfig();
        }
        val data = new Local(file, config, new Book());
        readPdfHeader(data);
        readPalmDocHeader(data);
        readMobiHeader(data);
        readExtHeader(data);
        System.out.println(getTrailingSize(data, 1));
        val flob = flobForRecord("", data, 1, true);
        val text = Texts.forFlob(flob, data.encoding, Texts.HTML);
        System.out.println(text);
        return data.book;
    }

    private void readPdfHeader(Local data) throws IOException, ParserException {
        val in = data.file;
        in.seek(60);
        data.ident = readString(in, 8, "ASCII");
        if (!"BOOKMOBI".equals(data.ident) && !"TEXTREAD".equals(data.ident)) {
            Log.d(TAG, "type and creator in PDF is not BOOKMOBI or TEXTREAD: {0}", data.ident);
            throw new ParserException("Unsupport MOBI file");
        }
        in.skipBytes(8);
        val count = in.readUnsignedShort();
        Log.t(TAG, "total pdb {0} records", count);
        data.records = new Record[count];
        Record previous = null;
        for (int i = 0; i < count; ++i) {
            val record = new Record(readUInt(in), in.readUnsignedByte(), in.readUnsignedByte(), in.readUnsignedShort());
            if (previous != null) {
                previous.size = record.offset - previous.offset;
            }
            previous = record;
            data.records[i] = record;
        }
        if (previous != null) {
            data.records[count - 1].size = in.length() - previous.offset;
        }
    }

    private void readPalmDocHeader(Local data) throws IOException, ParserException {
        val in = data.file;
        in.seek(data.records[0].offset);
        data.compressionType = in.readUnsignedShort();
        in.skipBytes(2);
        Attributes.setWords(data.book, in.readInt());
        data.textRecordCount = in.readUnsignedShort();
        Log.t(TAG, "number of text record: {0}", data.textRecordCount);
        data.textRecordSize = in.readUnsignedShort();
        Log.t(TAG, "maximum size of text record: {0}", data.textRecordSize);
        data.encryptionType = in.readUnsignedShort();
        in.skipBytes(2);
    }

    private void readMobiHeader(Local data) throws ParserException, IOException {
        val in = data.file;
        if ("TEXTREAD".equals(data.ident)) {
            data.encoding = "cp1252";
        }
        if (data.records[0].size <= 16) {
            Log.d(TAG, "not found MOBI header");
            data.encoding = "cp1252";
        } else {
            val curpos = in.getFilePointer();
            if (!"MOBI".equals(readString(in, 4, "ASCII"))) {
                Log.d(TAG, "not found MOBI header");
                return;
            }
            val headerLength = in.readInt();
            data.mobiType = in.readInt();
            Log.d(TAG, "mobi type is {0}", data.mobiType);
            detectEncoding(in.readInt(), data);
            data.book.getExtensions().set("mobi.uniqueId", in.readInt());
            data.book.getExtensions().set("mobi.version", in.readInt());
            in.skipBytes(40);
            data.textRecordEnd = in.readInt(); // not include this record
            Log.d(TAG, "text end record is {0}", data.textRecordEnd);
            int titleOffset = in.readInt();
            int titleLength = in.readInt();
            detectLanguage(in.readInt(), data);
            in.skipBytes(8);
            val mobiVersion = in.readInt();
            Log.d(TAG, "min version for mobi is {0}", mobiVersion);
            data.imageIndex = in.readInt();
            if (data.compressionType == 17480) {
                data.huffIndex = in.readInt();
                data.huffCount = in.readInt();
                in.skipBytes(8);
            } else {
                in.skipBytes(16);
            }
            data.exthFlags = in.readInt();
            in.skipBytes(60);
            data.firstContentIndex = in.readUnsignedShort();
            data.lastContentIndex = in.readUnsignedShort();
            in.skipBytes(4);
            data.fcisIndex = in.readInt();
            in.skipBytes(4);
            data.flisIndex = in.readInt();
            in.skipBytes(28);

            if ("TEXTREAD".equals(data.ident)
                    || headerLength < 0xE4
                    || headerLength > data.config.maxHeaderLength
                    || (data.config.fixExtraData && headerLength == 0xE4)) {
                data.extraFlags = 0;
                in.skipBytes(4);
            } else {
                data.extraFlags = in.readInt();
            }

            if (headerLength >= 0xF8) {
                data.ncxIndex = in.readInt();
                Log.t(TAG, "indx record is {0}", data.ncxIndex);
            }

            in.seek(data.records[0].offset + titleOffset);
            Attributes.setTitle(data.book, readString(in, titleLength, data.encoding));

            Log.t(TAG, "extra record data flags is {0}", data.extraFlags);
            in.seek(curpos + headerLength - 2);
            data.extraBytes = 2 * onebits(in.readUnsignedShort() & 0xFFFE);
        }
    }

    private void readExtHeader(Local data) throws ParserException, IOException {
        val in = data.file;
        if ((data.exthFlags & 0x40) == 0) {
            Log.d(TAG, "no EXTH header flag found");
            return;
        }
        if (!"EXTH".equals(readString(in, 4, "ASCII"))) {
            Log.d(TAG, "not found EXTH header");
            return;
        }
        val attributes = data.book.getAttributes();
        val extensions = data.book.getExtensions();
        in.skipBytes(4);
        val count = in.readInt();
        byte[] b = {};
        val authors = new LinkedList<String>();
        val keywords = new LinkedHashSet<String>();;
        int majorVersion = -1, minorVersion = -1, buildNumber = -1;
        for (int i = 0; i < count; ++i) {
            val type = in.readInt();
            val length = in.readInt() - 8;
            b = IOUtils.ensureLength(b, length);
            in.readFully(b, 0, length);
            String key;
            Object value = null;
            switch (type) {
            case 100:
                authors.add(new String(b, 0, length, data.encoding).trim());
                continue;
            case 101:
                key = Attributes.PUBLISHER;
            break;
            case 102:
                key = "imprint";
            break;
            case 103: {
                key = Attributes.INTRO;
                value = Texts.forString(new String(b, 0, length, data.encoding).trim(), Texts.PLAIN);
            }
            break;
            case 104:
                key = Attributes.ISBN;
                value = new String(b, 0, length, data.encoding).trim().replace("-", "");
            break;
            case 105:
                key = Attributes.GENRE;
                Collections.addAll(keywords, new String(b, 0, length, data.encoding).trim().split(";"));
                continue;
            case 106: {
                key = Attributes.PUBDATE;
                value = DateUtils.parse(new String(b, 0, length, data.encoding), "yyyy-m-D", new Date());
            }
            break;
            case 107:
                key = "review";
            break;
            case 108:
                key = Attributes.VENDOR;
            break;
            case 109:
                key = Attributes.RIGHTS;
            break;
            case 110:
                key = "subjectCode";
            break;
            case 111:
                key = "type";
            break;
            case 112: {
                key = "source";
                String str = new String(b, 0, length, data.encoding).trim();
                if (str.startsWith("urn:isbn:")) {
                    str = str.substring(9);
                    if (StringUtils.isNotEmpty(str)) {
                        Attributes.setISBN(data.book, str);
                        continue;
                    }
                } else if (str.startsWith("calibre:")) {
                    str = str.substring(8);
                    if (StringUtils.isNotEmpty(str)) {
                        key = "uuid";
                        value = str;
                    }
                }
            }
            break;
            case 113:
                key = "uuid";
            break;
            case 129:
                Log.t(TAG, "ignore 'KF8CoverURI': {0}", new String(b, 0, length, data.encoding));
                continue;
            case 201: {
                key = Attributes.COVER;
                if (data.imageIndex < 0) {
                    Log.e(TAG, "no image index found");
                    continue;
                }
                value = flobForRecord("cover.jpg", data, ByteUtils.getInt(b, 0) + data.imageIndex, false);
            }
            break;
            case 202: {
                if (data.imageIndex < 0) {
                    Log.e(TAG, "no image index found");
                    continue;
                }
                val index = ByteUtils.getInt(b, 0) + data.imageIndex;
                extensions.set("thumbnailCover", flobForRecord("thumbnail-cover.jpg", data, index, false));
                continue;
            }
            case 203: {
                if (data.imageIndex < 0) {
                    Log.e(TAG, "no image index found");
                    continue;
                }
                val index = ByteUtils.getInt(b, 0) + data.imageIndex;
                extensions.set("fakeCover", flobForRecord("fake-cover.jpg", data, index, false));
                continue;
            }
            case 204:
                extensions.set("mobi.creatorId", ByteUtils.getInt32(b, 0));
                continue;
            case 205:
                majorVersion = ByteUtils.getInt32(b, 0);
                continue;
            case 206:
                minorVersion = ByteUtils.getInt32(b, 0);
                continue;
            case 207:
                buildNumber = ByteUtils.getInt32(b, 0);
                continue;
            case 503:
                key = Attributes.TITLE;
            break;
            case 501:
                extensions.set("mobi.cdetype", new String(b, 0, length, data.encoding));
                continue;
            case 524: {
                key = Attributes.LANGUAGE;
                value = MiscUtils.parseLocale(new String(b, 0, length, data.encoding));
            }
            break;
            default:
                Log.d(TAG, "ignore record type: {0}", type);
                continue;
            }
            if (value == null) {
                value = new String(b, 0, length, data.encoding).trim();
            }
            attributes.set(key, value);
        }
        Attributes.setAuthors(data.book, authors);
        Attributes.setKeywords(data.book, keywords);
        if ((majorVersion | minorVersion | buildNumber) >= 0) {
            extensions.set("mobi.creatorVersion", majorVersion + "." + minorVersion + "." + buildNumber);
        }
    }

    private long readUInt(RandomAccessFile file) throws ParserException, IOException {
        return ByteUtils.getUInt32(readData(file, 4), 0);
    }

    private int getTrailingSize(Local data, int index) throws IOException {
        val in = data.file;
        int size = (int) data.records[index].size;
        in.seek(data.records[index + 1].offset);
        int num = 0;
        for (int flags = data.extraFlags; flags != 0; flags >>= 1) {
            if ((flags & 1) != 0) {
                num += getEntryTrailing(in, size - num);
            }
        }
        return num;
    }

    private int getEntryTrailing(RandomAccessFile in, int size) {
        int bitpos = 0, result = 0;
        while (true) {
            // TODO
        }
    }

    private void detectEncoding(int code, Local data) {
        switch (code) {
        case 65001:
            data.encoding = "UTF-8";
        break;
        case 1252:
            data.encoding = "CP1252";
        break;
        default:
            data.encoding = StringUtils.notEmptyOr(data.config.textEncoding, "CP1252");
        break;
        }
    }

    private void detectLanguage(int code, Local data) {
        val language = code & 0xFF;
        val dialect = (code >> 10) & 0xFF;
        Log.d(TAG, "language is {0}, dialect is {1}", language, dialect);
    }

    private Flob flobForRecord(String name, Local data, int index, boolean useCompression) throws IOException {
        val offset = data.records[index].offset;
        val end = data.records[index + 1].offset;
        val flob = Flobs.forBlock(name, data.file, offset, end - offset, null);
        if (data.compressionType == 2 && useCompression) {
            flob.size -= data.extraBytes;
            return new LZ77Flob(flob);
        } else {
            return flob;
        }
    }

    private int onebits(int x) {
        int count = 0;
        for (int i = 15; i >= 0; --i) {
            if (((x >> i) & 1) == 1) {
                ++count;
            }
        }
        return count;
    }

    @RequiredArgsConstructor
    private class Local {
        final RandomAccessFile file;
        final MobiInConfig config;
        final Book book;

        String ident;

        Record[] records;

        int compressionType;
        int encryptionType;

        int mobiType;

        String encoding;
        int textRecordCount;
        int textRecordSize;
        int textRecordEnd;
        int firstContentIndex;
        int lastContentIndex;

        int huffIndex, huffCount;

        int fcisIndex;
        int flisIndex;

        int ncxIndex;

        int imageIndex = -1;

        int exthFlags;

        int extraFlags, extraBytes;
    }

    @ToString
    @RequiredArgsConstructor
    private class Record {
        final long offset;
        long size;
        final int attrs;
        final int uid1;
        final int uid2;
    }

    private class ExtRecord {
        final int type;
        final long length;
        final String data;

        ExtRecord(RandomAccessFile file) throws IOException {
            type = file.readInt();
            length = file.readInt();
            data = "";
            byte[] buf = new byte[(int) (length - 8)];
            file.readFully(buf);
            System.out.println(type + ": " + new String(buf, "utf-8"));
        }
    }

    private class LZ77Flob extends FlobWrapper {

        LZ77Flob(BlockFlob flob) {
            super(flob);
        }

        @Override
        public InputStream openStream() throws IOException {
            val bb = Lz77Utils.decompress(getTarget().openStream());
            return new ByteArrayInputStream(bb.getDirectArray());
        }

        @Override
        public byte[] readAll() throws IOException {
            return Lz77Utils.decompress(getTarget().openStream()).toByteArray();
        }

        @Override
        public long writeTo(OutputStream out) throws IOException {
            val bb = Lz77Utils.decompress(getTarget().openStream());
            bb.writeTo(out);
            return bb.size();
        }

    }

}
