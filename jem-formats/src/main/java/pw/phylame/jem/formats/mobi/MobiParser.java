package pw.phylame.jem.formats.mobi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Date;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.epm.impl.BinaryParser;
import pw.phylame.jem.epm.util.ParserException;
import pw.phylame.jem.epm.util.config.NonConfig;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.flob.FlobWrapper;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.jem.util.text.Texts;
import pw.phylame.ycl.io.ByteUtils;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.io.LZ77Utils;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.DateUtils;
import pw.phylame.ycl.util.MiscUtils;

public class MobiParser extends BinaryParser<NonConfig> {
    private static final String TAG = MobiParser.class.getSimpleName();

    public MobiParser() {
        super("mobi", null);
    }

    @Override
    protected Book parse(RandomAccessFile file, NonConfig config) throws IOException, ParserException {
        val data = new Local(file, new Book());
        readPdfHeader(data);
        readPalmDocHeader(data);
        readMobiHeader(data);
        readExtHeader(data);
        val flob = flobForRecord("", data, data.firstContentIndex, true);
        val text = Texts.forFlob(flob, data.textEncoding, Texts.HTML);
        System.out.println(text);
        return data.book;
    }

    private void readPdfHeader(Local data) throws IOException, ParserException {
        val raf = data.raf;
        raf.seek(60);
        if (!"BOOKMOBI".equals(readString(raf, 8, "ASCII"))) {
            Log.d(TAG, "type and creator in PDF is not BOOKMOBI");
            throw new ParserException("Unsupport MOBI raf");
        }
        raf.skipBytes(8);
        val count = raf.readUnsignedShort();
        data.records = new Record[count];
        for (int i = 0; i < count; ++i) {
            data.records[i] = new Record(
                    readUInt(raf),
                    raf.readUnsignedByte(),
                    raf.readUnsignedByte(),
                    raf.readUnsignedShort());
        }
    }

    private void readPalmDocHeader(Local data) throws IOException, ParserException {
        val raf = data.raf;
        raf.seek(data.records[0].offset);
        data.compression = raf.readUnsignedShort();
        raf.skipBytes(2);
        Attributes.setWords(data.book, raf.readInt());
        data.textRecordCount = raf.readUnsignedShort();
        data.textRecordSize = raf.readUnsignedShort();
        if (data.compression == 17480) {
            data.encryption = raf.readUnsignedShort();
            raf.skipBytes(2);
        } else {
            data.currentPosition = raf.readInt();
        }
    }

    private void readMobiHeader(Local data) throws ParserException, IOException {
        val raf = data.raf;
        val curpos = raf.getFilePointer();
        if (!"MOBI".equals(readString(raf, 4, "ASCII"))) {
            Log.d(TAG, "not found MOBI header");
            return;
        }
        val headerLength = raf.readInt();
        Log.t(TAG, "length of mobi header is {0}", headerLength);
        data.mobiType = raf.readInt();
        Log.d(TAG, "mobi type is {0}", data.mobiType);
        if (data.mobiType != 2) {// Mobipocket Book
            throw new ParserException("Unsupport MOBI type: " + data.mobiType);
        }
        raf.skipBytes(2);
        data.textEncoding = detectEncoding(raf.readUnsignedShort());
        data.book.getExtensions().set("mobi.uniqueId", raf.readInt());
        data.book.getExtensions().set("mobi.version", raf.readInt());
        raf.skipBytes(40);
        data.textRecordEnd = raf.readInt(); // not include this record
        Log.d(TAG, "text end record is {0}", data.textRecordEnd);
        int titleOffset = raf.readInt();
        int titleLength = raf.readInt();
        Log.d(TAG, "locale code is {0}", raf.readInt());
        raf.skipBytes(8);
        Log.d(TAG, "min version for mobi is {0}", raf.readInt());
        data.imageIndex = raf.readInt();
        raf.skipBytes(16);
        data.extFlags = raf.readInt();
        raf.skipBytes(32);
        data.drmOffset = raf.readInt();
        data.drmCount = raf.readInt();
        data.drmSize = raf.readInt();
        data.drmFlags = raf.readInt();
        raf.skipBytes(12);
        data.firstContentIndex = raf.readUnsignedShort();
        data.lastContentIndex = raf.readUnsignedShort();
        raf.skipBytes(4);
        data.fcisIndex = raf.readInt();
        raf.skipBytes(4);
        data.flisIndex = raf.readInt();
        raf.skipBytes(32);
        data.ncxIndex = raf.readInt();

        raf.seek(data.records[0].offset + titleOffset);
        Attributes.setTitle(data.book, readString(raf, titleLength, data.textEncoding));

        raf.seek(curpos + headerLength);
    }

    private String detectEncoding(int code) throws ParserException {
        switch (code) {
        case 65001:
            return "UTF-8";
        case 1252:
            return "CP1252";
        default:
            throw new ParserException("Unknown encoding: " + code);
        }
    }

    private void readExtHeader(Local data) throws ParserException, IOException {
        val raf = data.raf;
        if ((data.extFlags & 0x40) == 0) {
            Log.d(TAG, "no EXTH header flag found");
            return;
        }
        if (!"EXTH".equals(readString(raf, 4, "ASCII"))) {
            Log.d(TAG, "not found EXTH header");
            return;
        }
        val attributes = data.book.getAttributes();
        val extensions = data.book.getExtensions();
        raf.skipBytes(4);
        val count = raf.readInt();
        byte[] b = {};
        int majorVersion = -1, minorVersion = -1, buildNumber = -1;
        for (int i = 0; i < count; ++i) {
            val type = raf.readInt();
            val length = raf.readInt() - 8;
            b = IOUtils.ensureLength(b, length);
            raf.readFully(b, 0, length);
            String key;
            Object value = null;
            switch (type) {
            case 100:
                key = Attributes.AUTHOR;
            break;
            case 101:
                key = Attributes.PUBLISHER;
            break;
            case 102:
                key = "imprint";
            break;
            case 103: {
                key = Attributes.INTRO;
                value = Texts.forString(new String(b, 0, length, data.textEncoding), Texts.PLAIN);
            }
            break;
            case 104:
                key = Attributes.ISBN;
            break;
            case 105:
                key = Attributes.GENRE;
            break;
            case 106: {
                key = Attributes.PUBDATE;
                value = DateUtils.parse(new String(b, 0, length, data.textEncoding), "yyyy-m-D", new Date());
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
            case 112:
                key = "source";
            break;
            case 113:
                key = "uuid";
            break;
            case 129:
                Log.t(TAG, "ignore 'KF8CoverURI': {0}", new String(b, 0, length, data.textEncoding));
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
                extensions.set("mobi.cdetype", new String(b, 0, length, data.textEncoding));
                continue;
            case 524: {
                key = Attributes.LANGUAGE;
                value = MiscUtils.parseLocale(new String(b, 0, length, data.textEncoding));
            }
            break;
            default:
                Log.d(TAG, "ignore record type: {0}", type);
                continue;
            }
            if (value == null) {
                value = new String(b, 0, length, data.textEncoding);
            }
            attributes.set(key, value);
        }
        if ((majorVersion | minorVersion | buildNumber) >= 0) {
            extensions.set("mobi.creatorVersion", majorVersion + "." + minorVersion + "." + buildNumber);
        }
    }

    private long readUInt(RandomAccessFile file) throws ParserException, IOException {
        return ByteUtils.getUInt32(readData(file, 4), 0);
    }

    private Flob flobForRecord(String name, Local data, int index, boolean useCompression) throws IOException {
        val offset = data.records[index].offset;
        val end = data.records[index + 1].offset;
        val flob = Flobs.forBlock(name, data.raf, offset, end - offset, null);
        if (data.compression == 2 && useCompression) {
            return new LZ77Flob(flob);
        } else {
            return flob;
        }
    }

    @RequiredArgsConstructor
    private class Local {
        final RandomAccessFile raf;
        final Book book;

        Record[] records;

        int compression;
        int encryption = 0;

        int mobiType;

        int currentPosition = 0;
        String textEncoding;
        int textRecordCount;
        int textRecordSize;
        int textRecordEnd;
        int firstContentIndex;
        int lastContentIndex;

        int fcisIndex;
        int flisIndex;

        int ncxIndex;

        int imageIndex = -1;

        int extFlags;

        int drmOffset, drmCount, drmSize, drmFlags;
    }

    @ToString
    @RequiredArgsConstructor
    private class Record {
        final long offset;
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

        LZ77Flob(Flob flob) {
            super(flob);
        }

        @Override
        public InputStream openStream() throws IOException {
            val bb = LZ77Utils.uncompress(super.openStream());
            return new ByteArrayInputStream(bb.getDirectArray());
        }

        @Override
        public byte[] readAll() throws IOException {
            return LZ77Utils.uncompress(super.openStream()).toByteArray();
        }

        @Override
        public long writeTo(OutputStream out) throws IOException {
            val bb = LZ77Utils.uncompress(super.openStream());
            bb.writeTo(out);
            return bb.size();
        }

    }

}
