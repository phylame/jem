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

package pw.phylame.jem.formats.pmab;

import lombok.NonNull;
import lombok.val;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.epm.base.ZipParser;
import pw.phylame.jem.epm.util.NumberUtils;
import pw.phylame.jem.epm.util.ParserException;
import pw.phylame.jem.epm.util.ZipUtils;
import pw.phylame.jem.formats.util.JFMessages;
import pw.phylame.jem.formats.util.TestUtils;
import pw.phylame.jem.util.Variants;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.jem.util.text.Text;
import pw.phylame.jem.util.text.Texts;
import pw.phylame.ycl.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

import static pw.phylame.jem.epm.util.xml.XmlUtils.attributeOf;
import static pw.phylame.jem.epm.util.xml.XmlUtils.newPullParser;
import static pw.phylame.ycl.util.StringUtils.*;

/**
 * PMAB e-book parser.
 */
public class PmabParser extends ZipParser<PmabInConfig> {

    public PmabParser() {
        super("pmab", PmabInConfig.class);
    }

    @Override
    public Book parse(@NonNull ZipFile zip, PmabInConfig config) throws IOException, ParserException {
        if (!PMAB.MT_PMAB.equals(ZipUtils.readString(zip, PMAB.MIME_FILE, "ASCII"))) {
            throw new ParserException(JFMessages.tr("pmab.parse.invalidMT", PMAB.MIME_FILE, PMAB.MT_PMAB));
        }
        if (config == null) {
            config = new PmabInConfig();
        }
        val tuple = new Tuple(new Book(), zip, config, newPullParser(false));
        readPBM(tuple);
        readPBC(tuple);
        return tuple.book;
    }

    private int getVersion(XmlPullParser xpp, String error) throws ParserException {
        val str = attributeOf(xpp, "version");
        switch (str) {
            case "3.0":
                return 3;
            case "2.0":
                return 2;
            default:
                throw new ParserException(JFMessages.tr(error));
        }
    }

    private void readPBM(Tuple tuple) throws IOException, ParserException {
        int version = 0;
        boolean hasText = false;
        tuple.inAttributes = false;
        val b = new StringBuilder();
        val xpp = tuple.xpp;
        try (val in = ZipUtils.openStream(tuple.zip, PMAB.PBM_FILE)) {
            xpp.setInput(in, null);
            int event = xpp.getEventType();
            do {
                switch (event) {
                    case XmlPullParser.START_TAG: {
                        val tag = xpp.getName();
                        if (version == 3) {
                            hasText = startPBMv3(tag, tuple);
                        } else if (version == 2) {
                            hasText = startPBMv2(tag, tuple);
                        } else if (tag.equals("pbm")) {
                            tuple.pbmVersion = version = getVersion(xpp, "pmab.parse.unsupportedPBM");
                        } else {
                            hasText = false;
                        }
                    }
                    break;
                    case XmlPullParser.TEXT: {
                        if (hasText) {
                            b.append(xpp.getText());
                        }
                    }
                    break;
                    case XmlPullParser.END_TAG: {
                        val tag = xpp.getName();
                        if (version == 3) {
                            endPBMv3(tag, b, tuple);
                        } else if (version == 2) {
                            endPBMv2(tag, b, tuple);
                        }
                        b.setLength(0);
                    }
                    break;
                }
                event = xpp.next();
            } while (event != XmlPullParser.END_DOCUMENT);
        } catch (XmlPullParserException e) {
            throw new ParserException(JFMessages.tr("pmab.parse.invalidPBM", e.getLocalizedMessage()), e);
        }
    }

    private Object parseV3Item(String text, Tuple tuple) throws IOException, ParserException {
        val itemType = tuple.itemType;
        Object value;
        if (isEmpty(itemType)) { // no type specified, text as string
            value = text;
        } else {
            val type = firstPartOf(itemType, ';');
            if (type.equals(Variants.STRING)) {
                value = tuple.itemName.equals(Attributes.LANGUAGE) ? TestUtils.parseLocale(text) : text;
            } else if (type.equals(Variants.DATETIME) || type.equals("date") || type.equals("time")) {
                value = TestUtils.parseDate(text, valueOfName(itemType, "format", ";", false, tuple.config.dateFormat));
            } else if (type.startsWith("text/")) {  // text object
                val t = type.substring(5);
                Flob flob = Flobs.forZip(tuple.zip, text, "text/" + t);
                value = Texts.forFile(flob, valueOfName(itemType, "encoding", ";", false, tuple.config.textEncoding), t);
            } else if (type.equals(Variants.LOCALE)) {
                value = TestUtils.parseLocale(text);
            } else if (type.matches("[\\w]+/[\\w\\-]+")) {   // file object
                value = Flobs.forZip(tuple.zip, text, type);
            } else if (type.equals(Variants.INTEGER) || type.equals("uint")) {
                value = NumberUtils.parseInt(text);
            } else if (type.equals(Variants.REAL)) {
                value = NumberUtils.parseDouble(text);
            } else if (type.equals("bytes")) {
                Log.w(getName(), "***PMAB: <item> with 'bytes' type is ignored***");
                value = text;
            } else if (type.equals(Variants.BOOLEAN)) {
                value = Boolean.parseBoolean(text);
            } else {    // store as string
                value = text;
            }
        }
        return value;
    }

    private boolean startPBMv3(String tag, Tuple tuple) throws ParserException {
        val xpp = tuple.xpp;
        boolean hasText = false;
        switch (tag) {
            case "item": {
                tuple.itemName = attributeOf(xpp, "name");
                tuple.itemType = xpp.getAttributeValue(null, "type");
                hasText = true;
            }
            break;
            case "attributes":
                tuple.inAttributes = true;
                break;
            case "meta":
                tuple.metadata.put(attributeOf(xpp, "name"), attributeOf(xpp, "value"));
                break;
            case "head":
                tuple.metadata = new HashMap<>();
                break;
        }
        return hasText;
    }

    private void endPBMv3(String tag, StringBuilder b, Tuple tuple) throws IOException, ParserException {
        if (tag.equals("item")) {
            val value = parseV3Item(b.toString().trim(), tuple);
            if (tuple.inAttributes) {
                tuple.book.getAttributes().put(tuple.itemName, value);
            } else {
                tuple.book.getExtensions().put(tuple.itemName, value);
            }
        } else if (tag.equals("attributes")) {
            tuple.inAttributes = false;
        }
    }

    private boolean startPBMv2(String tag, Tuple tuple) throws IOException, ParserException {
        val xpp = tuple.xpp;
        boolean hasText = false;
        switch (tag) {
            case "attr": {
                if (tuple.checkCount()) {
                    tuple.attrName = attributeOf(xpp, "name");
                    tuple.mediaType = tuple.attrName.equals(Attributes.COVER) ? xpp.getAttributeValue(null, "media-type") : null;
                    hasText = true;
                }
            }
            break;
            case "item": {
                if (tuple.checkCount()) {
                    val name = attributeOf(xpp, "name");
                    val type = xpp.getAttributeValue(null, "type");
                    if (isEmpty(type) || type.equals(Variants.TEXT)) {
                        tuple.book.getExtensions().put(name, attributeOf(xpp, "value"));
                    } else if (type.equals("number")) {
                        tuple.book.getExtensions().put(name, NumberUtils.parseNumber(attributeOf(xpp, "value")));
                    } else if (type.equals(Variants.FLOB)) {    // file will be processed in <object>
                        tuple.attrName = name;
                    } else {
                        throw new ParserException(JFMessages.tr("pmab.parse.2.unknownItemType", type));
                    }
                }
            }
            break;
            case "object": {
                if (tuple.checkCount()) {
                    val mime = attributeOf(xpp, "media-type");
                    val flob = Flobs.forZip(tuple.zip, attributeOf(xpp, "href"), mime);
                    Object value = flob;
                    if (mime.startsWith("text/plain")) {
                        val encoding = xpp.getAttributeValue(null, "encoding");
                        value = isEmpty(encoding)
                                ? Texts.forFile(flob, tuple.config.textEncoding, Text.PLAIN)
                                : Texts.forFile(flob, encoding, Text.PLAIN);
                    }
                    tuple.book.getExtensions().put(tuple.attrName, value);
                }
            }
            break;
            case "metadata": {
                val str = xpp.getAttributeValue(null, "count");
                tuple.count = isNotEmpty(str) ? NumberUtils.parseInt(str) : -1;
                tuple.order = 0;
            }
            break;
            case "extension": {
                val str = xpp.getAttributeValue(null, "count");
                tuple.count = isNotEmpty(str) ? NumberUtils.parseInt(str) : -1;
                tuple.order = 0;
            }
            break;
            case "meta":
                tuple.metadata.put(attributeOf(xpp, "name"), attributeOf(xpp, "content"));
                break;
            case "head":
                tuple.metadata = new HashMap<>();
                break;
        }
        return hasText;
    }

    private void endPBMv2(String tag, StringBuilder b, Tuple tuple) throws IOException, ParserException {
        val attrName = tuple.attrName;
        if (tag.equals("attr")) {
            if (tuple.checkCount()) {
                val text = b.toString().trim();
                Object value;
                if (tuple.attrName.equals(Attributes.DATE)) {
                    value = TestUtils.parseDate(text, tuple.config.dateFormat);
                } else if (attrName.equals(Attributes.INTRO)) {
                    value = Texts.forString(text, Text.PLAIN);
                } else if (attrName.equals(Attributes.LANGUAGE)) {
                    value = TestUtils.parseLocale(text);
                } else if (isNotEmpty(tuple.mediaType)) {
                    value = Flobs.forZip(tuple.zip, text, tuple.mediaType);
                } else {
                    value = text;
                }
                tuple.book.getAttributes().put(attrName, value);
            }
            ++tuple.order;
        } else if (tag.equals("item")) {
            ++tuple.order;
        }
    }

    private void readPBC(Tuple tuple) throws IOException, ParserException {
        val xpp = tuple.xpp;
        int version = 0;
        boolean hasText = false;
        val b = new StringBuilder();
        try (val in = ZipUtils.openStream(tuple.zip, PMAB.PBC_FILE)) {
            xpp.setInput(in, null);
            int event = xpp.getEventType();
            do {
                switch (event) {
                    case XmlPullParser.START_TAG: {
                        val tag = xpp.getName();
                        if (version == 3) {
                            hasText = startPBCv3(tag, tuple);
                        } else if (version == 2) {
                            hasText = startPBCv2(tag, tuple);
                        } else if (tag.equals("pbc")) {
                            version = getVersion(xpp, "pmab.parse.unsupportedPBC");
                        } else {
                            hasText = false;
                        }
                    }
                    break;
                    case XmlPullParser.TEXT: {
                        if (hasText) {
                            b.append(xpp.getText());
                        }
                    }
                    break;
                    case XmlPullParser.END_TAG: {
                        val tag = xpp.getName();
                        if (version == 3) {
                            endPBCv3(tag, b, tuple);
                        } else if (version == 2) {
                            endPBCv2(tag, b, tuple);
                        }
                        b.setLength(0);
                    }
                    break;
                    case XmlPullParser.START_DOCUMENT: {
                        tuple.currentChapter = tuple.book;
                    }
                    break;
                }
                event = xpp.next();
            } while (event != XmlPullParser.END_DOCUMENT);
        } catch (XmlPullParserException e) {
            throw new ParserException(JFMessages.tr("pmab.parse.invalidPBC", e.getLocalizedMessage()), e);
        }
    }

    private boolean startPBCv3(String tag, Tuple tuple) throws ParserException {
        val xpp = tuple.xpp;
        boolean hasText = false;
        switch (tag) {
            case "chapter":
                tuple.appendChapter();
                break;
            case "item": {
                tuple.itemName = attributeOf(xpp, "name");
                tuple.itemType = xpp.getAttributeValue(null, "type");
                hasText = true;
            }
            break;
            case "content": {
                tuple.itemType = xpp.getAttributeValue(null, "type");
                hasText = true;
            }
            break;
        }
        return hasText;
    }

    private void endPBCv3(String tag, StringBuilder b, Tuple tuple) throws IOException, ParserException {
        val itemType = tuple.itemType;
        switch (tag) {
            case "chapter":
                tuple.currentChapter = tuple.currentChapter.getParent();
                break;
            case "item": {
                String text = b.toString().trim();
                tuple.currentChapter.getAttributes().put(tuple.itemName, parseV3Item(text, tuple));
            }
            break;
            case "content": {
                Text text;
                val data = b.toString().trim();
                if (isEmpty(itemType)) {
                    text = Texts.forString(data, Text.PLAIN);
                } else if (itemType.startsWith("text/")) {
                    val flob = Flobs.forZip(tuple.zip, data, firstPartOf(itemType, ';'));
                    text = Texts.forFile(flob, valueOfName(itemType, "encoding", ";", false, tuple.config.textEncoding), Text.PLAIN);
                } else {
                    text = Texts.forString(data, Text.PLAIN);
                }
                tuple.currentChapter.setText(text);
            }
            break;
        }
    }

    private boolean startPBCv2(String tag, Tuple tuple) throws IOException, ParserException {
        val xpp = tuple.xpp;
        boolean hasText = false;
        switch (tag) {
            case "chapter": {
                val href = xpp.getAttributeValue(null, "href");
                if (isEmpty(href)) {
                    tuple.appendChapter();
                } else {
                    val flob = Flobs.forZip(tuple.zip, href, "text/plain");
                    tuple.chapterEncoding = xpp.getAttributeValue(null, "encoding");
                    if (isEmpty(tuple.chapterEncoding)) {
                        tuple.chapterEncoding = tuple.config.textEncoding;
                    }
                    tuple.appendChapter();
                    tuple.currentChapter.setText(Texts.forFile(flob, tuple.chapterEncoding, Text.PLAIN));
                }
            }
            break;
            case "title":
                hasText = true;
                break;
            case "cover": {
                val href = attributeOf(xpp, "href");
                val mime = attributeOf(xpp, "media-type");
                Attributes.setCover(tuple.currentChapter, Flobs.forZip(tuple.zip, href, mime));
            }
            break;
            case "intro": {
                val flob = Flobs.forZip(tuple.zip, attributeOf(xpp, "href"), "text/plain");
                String encoding = xpp.getAttributeValue(null, "encoding");
                if (isEmpty(encoding)) {
                    encoding = tuple.config.useChapterEncoding ? tuple.chapterEncoding : tuple.config.textEncoding;
                }
                Attributes.setIntro(tuple.currentChapter, Texts.forFile(flob, encoding, Text.PLAIN));
            }
            break;
        }
        return hasText;
    }

    private void endPBCv2(String tag, StringBuilder b, Tuple tuple) {
        if (tag.equals("chapter")) {
            tuple.currentChapter = tuple.currentChapter.getParent();
        } else if (tag.equals("title")) {
            Attributes.setTitle(tuple.currentChapter, b.toString().trim());
        }
    }

    private class Tuple {
        private Book book;
        private ZipFile zip;
        private PmabInConfig config;
        private XmlPullParser xpp;

        private Tuple(Book book, ZipFile zip, PmabInConfig config, XmlPullParser xpp) {
            this.book = book;
            this.zip = zip;
            this.config = config;
            this.xpp = xpp;
        }

        private int pbmVersion, pbcVersion;

        // PBM 3 data
        private String itemName, itemType;      // item attribute
        private boolean inAttributes = false;   // item is contained in <attributes>
        // PMAB 2 counter
        private int count, order;
        // PBM 2 data
        private String attrName, mediaType;
        // pbc data
        private Chapter currentChapter;
        // used for encoding of intro in chapter
        private String chapterEncoding;

        private Map<String, Object> metadata;

        private boolean checkCount() {
            return count < 0 || order < count;
        }

        private void appendChapter() {
            val chapter = new Chapter();
            currentChapter.append(chapter);
            currentChapter = chapter;
        }
    }
}