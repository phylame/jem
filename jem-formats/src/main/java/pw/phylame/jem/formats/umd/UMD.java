/*
 * Copyright 2016 Peng Wan <phylame@163.com>
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


import pw.phylame.jem.formats.util.JFMessages;

/**
 * Defines constants and methods.
 */
public final class UMD {
    static final long MAGIC_NUMBER = 0xde9a9b89L;

    static final String TEXT_ENCODING = "UTF-16LE";

    static final int CHUNK_SEPARATOR = 0x23;     // "#"
    static final int ADDITION_SEPARATOR = 0x24;     // "$"

    static final String UMD_LINE_FEED = "\u2029";

    // content type
    public static final int TEXT = 0x1;
    public static final int CARTOON = 0x2;
    public static final int COMIC = 0x3;

    public static String nameOfType(int type) {
        String key;
        switch (type) {
            case TEXT:
                key = "text";
                break;
            case CARTOON:
                key = "cartoon";
                break;
            default:
                key = "comic";
                break;
        }
        return JFMessages.tr("umd.info." + key);
    }

    // content type
    static final int CONTENT_SINGLE = 0;
    static final int CONTENT_APPENDED = 1;

    // image format
    static final int IMAGE_TYPE_BMP = 0;
    static final int IMAGE_TYPE_GIF = 2;
    static final int IMAGE_TYPE_JPG = 1;

    static String nameOfFormat(int type) {
        switch (type) {
            case IMAGE_TYPE_BMP:
                return "bmp";
            case IMAGE_TYPE_GIF:
                return "gif";
            default:
                return "jpg";
        }
    }

    static int formatOfName(String format) {
        if ("bmp".equals(format)) {
            return IMAGE_TYPE_BMP;
        } else if ("git".equals(format)) {
            return IMAGE_TYPE_GIF;
        } else {
            return IMAGE_TYPE_JPG;
        }
    }

    // text buffer size, 32KB
    static final int BLOCK_SIZE = 0x8000;

    // data chunk type id
    static final int CDT_UMD_HEAD = 0x1;
    static final int CDT_TITLE = 0x2;
    static final int CDT_AUTHOR = 0x3;
    static final int CDT_YEAR = 0x4;
    static final int CDT_MONTH = 0x5;
    static final int CDT_DAY = 0x6;
    static final int CDT_GENRE = 0x7;
    static final int CDT_PUBLISHER = 0x8;
    static final int CDT_VENDOR = 0x9;
    static final int CDT_CONTENT_LENGTH = 0xB;  // uncompressed text bytes size
    static final int CDT_CHAPTER_OFFSET = 0x83;
    static final int CDT_CHAPTER_TITLE = 0x84;
    static final int CDT_IMAGE_FORMAT = 0xE;
    static final int CDT_CONTENT_ID = 0xA;
    static final int CDT_LICENSE_KEY = 0xF1;
    static final int CDT_CONTENT_END = 0x81;
    static final int CDT_COVER_IMAGE = 0x82;
    static final int CDT_PAGE_OFFSET = 0x87;
    static final int CDT_CDS_KEY = 0xF0;
    static final int CDT_UMD_END = 0xC;
}

