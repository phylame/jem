package pw.phylame.jem.formats.mobi;

import java.io.IOException;
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
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.jem.util.text.Texts;
import pw.phylame.ycl.io.ByteUtils;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.DateUtils;

public class MobiParser extends BinaryParser<NonConfig> {
    private static final String TAG = MobiParser.class.getSimpleName();

    public MobiParser() {
        super("mobi", null);
    }

    @Override
    protected Book parse(RandomAccessFile file, NonConfig config) throws IOException, ParserException {
        val data = new Local(file, new Book());
        readHeader(data);
        readPalmDocHeader(data);
        readMobiHeader(data);
        readExtHeader(data);
        return data.book;
    }

    private void readHeader(Local data) throws IOException, ParserException {
        val file = data.file;
        val book = data.book;
        file.seek(32);
        val extensions = book.getExtensions();
        extensions.set("mobi.attributes", file.readShort());
        extensions.set("mobi.version", file.readShort());
        Attributes.setPubdate(book, new Date(readUInt(file) * 1000));
        Attributes.setDate(book, new Date(readUInt(file) * 1000));
        file.skipBytes(32);
        val count = file.readUnsignedShort();
        data.records = new Record[count];
        for (int i = 0; i < count; ++i) {
            data.records[i] = new Record(readUInt(file), file.readUnsignedByte(), readData(file, 3));
        }
    }

    private void readPalmDocHeader(Local data) throws IOException, ParserException {
        val file = data.file;
        file.seek(data.records[0].offset);
        data.compression = file.readUnsignedShort();
        file.skipBytes(2);
        Attributes.setWords(data.book, file.readInt());
        data.textRecordCount = file.readUnsignedShort();
        data.textRecordSize = file.readUnsignedShort();
        data.encryption = file.readUnsignedShort();
        file.skipBytes(2);
    }

    private void readMobiHeader(Local data) throws ParserException, IOException {
        val file = data.file;
        val curpos = file.getFilePointer();
        if (file.readInt() != 0x4D4F4249) {
            Log.d(TAG, "not found MOBI header");
            return;
        }
        int length = file.readInt();
        data.mobiType = file.readInt();
        Log.d(TAG, "mobi type is {0}", data.mobiType);
        data.textEncoding = detectEncoding(file.readInt());
        file.skipBytes(48);
        data.textRecordEnd = file.readInt();
        Log.d(TAG, "end index of text record is {0}", data.textRecordEnd);
        int titleOffset = file.readInt();
        int titleLength = file.readInt();
        Log.d(TAG, "locale code is {0}", file.readInt());
        file.skipBytes(12);
        data.imageIndex = file.readInt();
        file.skipBytes(16);
        data.extFlags = file.read();
        file.skipBytes(36);
        data.drmOffset = file.readInt();
        data.drmCount = file.readInt();
        data.drmSize = file.readInt();
        data.drmFlags = file.readInt();

        file.seek(data.records[0].offset + titleOffset);
        Attributes.setTitle(data.book, readString(file, titleLength, data.textEncoding));

        file.seek(curpos + length);
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
        val file = data.file;
        val book = data.book;
        if (file.readInt() != 0x45585448) {
            Log.d(TAG, "no EXTH header found");
            return;
        }
        file.skipBytes(4);
        data.extRecords = new ExtRecord[file.readInt()];
        byte[] buf = {};
        for (int i = 0; i < data.extRecords.length; ++i) {
            val type = file.readInt();
            val length = file.readInt() - 8;
            buf = ensureLength(buf, length);
            file.readFully(buf, 0, length);
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
                value = Texts.forString(new String(buf, 0, length, data.textEncoding), Texts.PLAIN);
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
                value = DateUtils.parse(new String(buf, 0, length, data.textEncoding), "yyyy-mm-DD HH:MM:SS",
                        new Date());
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
            case 111:
                key = "type";
            break;
            case 503:
                key = Attributes.TITLE;
            break;
            case 112:
                key = Attributes.PRICE;
            break;
            case 113:
                key = "uuid";
            break;
            case 201: {
                key = Attributes.COVER;
                if (data.imageIndex < 0) {
                    Log.e(TAG, "no image index found");
                    continue;
                }
                val offset = data.records[data.imageIndex].offset;
                val end = data.records[data.imageIndex + 1].offset;
                value = Flobs.forBlock("cover.jpg", file, offset, end - offset, null);
            }
            break;
            default:
                Log.d(TAG, "ignore record type: {0}", type);
                continue;
            }
            if (value == null) {
                value = new String(buf, 0, length, data.textEncoding);
            }
            book.getAttributes().set(key, value);
        }
    }

    private long readUInt(RandomAccessFile file) throws ParserException, IOException {
        return ByteUtils.getUInt32(readData(file, 4), 0);
    }

    @RequiredArgsConstructor
    private class Local {
        final RandomAccessFile file;
        final Book book;

        Record[] records;

        int compression;
        int encryption;

        int mobiType;

        String textEncoding;
        int textRecordCount;
        int textRecordSize;
        int textRecordEnd;

        int imageIndex = -1;

        int extFlags;

        int drmOffset, drmCount, drmSize, drmFlags;

        ExtRecord[] extRecords;
    }

    @ToString
    @RequiredArgsConstructor
    private class Record {
        final long offset;
        final int attrs;
        final byte[] id;
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

}
