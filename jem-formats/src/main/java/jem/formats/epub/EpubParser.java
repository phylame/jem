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

package jem.formats.epub;

import jclp.io.PathUtils;
import jclp.log.Log;
import jclp.util.DateUtils;
import jclp.util.MiscUtils;
import jclp.vdm.VdmReader;
import jem.Attributes;
import jem.Book;
import jem.Chapter;
import jem.epm.impl.VamParser;
import jem.epm.util.ParserException;
import jem.epm.util.VamUtils;
import jem.formats.util.M;
import jem.util.flob.Flobs;
import jem.util.text.Texts;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import static jclp.util.StringUtils.*;
import static jem.epm.util.xml.XmlUtils.attributeOf;
import static jem.epm.util.xml.XmlUtils.newPullParser;

public class EpubParser extends VamParser<EpubInConfig> {
    private static final String TAG = EpubParser.class.getSimpleName();

    public EpubParser() {
        super("epub", EpubInConfig.class);
    }

    @Override
    public Book parse(VdmReader vam, EpubInConfig config) throws IOException, ParserException {
        if (!EPUB.MT_EPUB.equals(VamUtils.textOf(vam, EPUB.MIME_FILE, "ASCII").trim())) {
            throw new ParserException(M.tr("epub.parse.invalidMT", EPUB.MIME_FILE, EPUB.MT_EPUB));
        }
        if (config == null) {
            config = new EpubInConfig();
        }
        val data = new Local(new Book(), vam, config);
        loadOpf(data);
        readOpf(data);
        readNcx(data);
        return data.book;
    }

    private void loadOpf(Local data) throws ParserException, IOException {
        val xpp = newPullParser(false);
        try (val in = VamUtils.streamFor(data.vam, EPUB.CONTAINER_FILE)) {
            xpp.setInput(in, null);
            int event = xpp.getEventType();
            do {
                switch (event) {
                    case XmlPullParser.START_TAG: {
                        if (xpp.getName().equals("rootfile")) {
                            val mime = attributeOf(xpp, "media-type");
                            if (mime.equals(EPUB.MT_OPF)) {
                                data.opfPath = attributeOf(xpp, "full-path");
                                break;
                            }
                        }
                    }
                    break;
                }
                event = xpp.next();
            } while (event != XmlPullParser.END_DOCUMENT);
        } catch (XmlPullParserException e) {
            throw new ParserException(M.tr("epub.parse.invalidContainer", e.getLocalizedMessage()), e);
        }
        data.opsDir = PathUtils.dirName(data.opfPath);
    }

    private void readOpf(Local data) throws ParserException, IOException {
        if (isEmpty(data.opfPath)) {
            throw new ParserException(M.tr("epub.parse.noOpf"));
        }
        String coverId = null;
        val b = new StringBuilder();
        val xpp = newPullParser(false);
        try (val in = VamUtils.streamFor(data.vam, data.opfPath)) {
            xpp.setInput(in, null);
            boolean hasText = false;
            int event = xpp.getEventType();
            do {
                val tag = xpp.getName();
                switch (event) {
                    case XmlPullParser.START_TAG: {
                        hasText = false;
                        if (tag.equals("item")) {
                            data.items.put(attributeOf(xpp, "id"),
                                    new Item(attributeOf(xpp, "href"), attributeOf(xpp, "media-type")));
                        } else if (tag.equals("itemref")) {
                            // ignored
                        } else if (tag.equals("reference")) {
                            // ignored
                        } else if (tag.equals("meta")) {
                            val name = xpp.getAttributeValue(null, "name");
                            val value = xpp.getAttributeValue(null, "content");
                            if ("cover".equals(name)) {
                                coverId = value;
                            } else if (value != null) {
                                data.book.getAttributes().set(name, value);
                            }
                        } else if (tag.startsWith("dc:")) {
                            data.scheme = xpp.getAttributeValue(null, "opf:scheme");
                            data.role = xpp.getAttributeValue(null, "opf:role");
                            data.event = xpp.getAttributeValue(null, "opf:event");
                            hasText = true;
                        } else if (tag.equals("package")) {
                            val version = attributeOf(xpp, "version");
                            if (!version.startsWith("2")) {
                                throw new ParserException(M.tr("epub.make.unsupportedVersion", version));
                            }
                        } else if (tag.equals("spine")) {
                            data.tocId = xpp.getAttributeValue(null, "toc");
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
                        if (tag.startsWith("dc:")) {
                            parseMetadata(tag.substring(3), xpp, b, data);
                        }
                        b.setLength(0);
                    }
                    break;
                }
                event = xpp.next();
            } while (event != XmlPullParser.END_DOCUMENT);
        } catch (XmlPullParserException e) {
            throw new ParserException(M.tr("epub.parse.invalidOpf", e.getLocalizedMessage()), e);
        }
        if (isNotEmpty(coverId)) {
            val item = data.items.remove(coverId);
            if (item != null) {
                Attributes.setCover(data.book, Flobs.forVam(data.vam, data.opsDir + '/' + item.href, null));
            }
        }
    }

    private void parseMetadata(String name, XmlPullParser xpp, StringBuilder b, Local data) {
        val book = data.book;
        val text = trimmed(b.toString());
        Object value = text;
        switch (name) {
            case "identifier": {
                if ("uuid".equals(data.scheme)) {
                    name = "uuid";
                } else if ("isbn".equals(data.scheme)) {
                    name = Attributes.ISBN;
                }
            }
            break;
            case "creator": {
                if (data.role == null) {
                    name = Attributes.AUTHOR;
                } else if (data.role.equals("aut")) {
                    name = Attributes.AUTHOR;
                }
            }
            break;
            case "date": {
                if (data.event == null) {
                    name = Attributes.PUBDATE;
                } else if (data.event.equals("creation")) {
                    name = Attributes.PUBDATE;
                } else if (data.event.equals("modification")) {
                    name = Attributes.DATE;
                }
                try {
                    value = DateUtils.parse(text, "yyyy-m-D");
                } catch (ParseException e) {
                    Log.d(TAG, "invalid date format", e);
                    return;
                }
            }
            break;
            case "contributor": {
                if (data.role == null) {
                    name = Attributes.VENDOR;
                } else if (data.role.equals("bkp")) {
                    name = Attributes.VENDOR;
                }
            }
            break;
            case "type":
            case "subject": {
                name = Attributes.GENRE;
            }
            break;
            case "description": {
                name = Attributes.INTRO;
                value = Texts.forString(text, Texts.PLAIN);
            }
            break;
            case "language": {
                value = MiscUtils.parseLocale(text);
            }
            break;
        }
        book.getAttributes().set(name, value);
    }

    private void readNcx(Local data) throws ParserException, IOException {
        if (isEmpty(data.tocId)) {
            Log.d(TAG, "no toc resource found");
            return;
        }
        Item item = data.items.remove(data.tocId);
        if (item == null) {
            Log.d(TAG, "no toc resource found for id: {0}", data.tocId);
            return;
        }
        val book = data.book;
        val b = new StringBuilder();
        val xpp = newPullParser(false);
        try (val in = VamUtils.streamFor(data.vam, data.opsDir + '/' + item.href)) {
            xpp.setInput(in, null);
            boolean hasText = false;
            boolean forChapter = false;
            Chapter chapter = book;
            int event = xpp.getEventType();
            do {
                val tag = xpp.getName();
                switch (event) {
                    case XmlPullParser.START_TAG: {
                        hasText = false;
                        if (tag.equals("navPoint")) {
                            val id = attributeOf(xpp, "id");
                            item = data.items.remove(id);
                            if (item == null) {
                                Log.d(TAG, "no such resource with id: {0}", id);
                            }
                            val sub = new Chapter();
                            chapter.append(sub);
                            chapter = sub;
                        } else if (tag.equals("content")) {
                            val href = data.opsDir + '/' + attributeOf(xpp, "src");
                            String mime, type;
                            if (item == null) {
                                mime = "text/plain";
                                type = Texts.PLAIN;
                            } else {
                                mime = item.mime;
                                type = mime.contains("html") ? Texts.HTML : Texts.PLAIN;
                            }
                            chapter.setText(Texts.forFlob(Flobs.forVam(data.vam, href, mime), null, type));
                        } else if (tag.equals("text")) {
                            hasText = true;
                        } else if (tag.equals("meta")) {
                            book.getExtensions().set(attributeOf(xpp, "name"), attributeOf(xpp, "content"));
                        } else if (tag.equals("navLabel")) {
                            forChapter = true;
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
                        if (tag.equals("navPoint")) {
                            chapter = chapter.getParent();
                        } else if (tag.equals("text")) {
                            if (forChapter) {
                                Attributes.setTitle(chapter, trimmed(b.toString()));
                            }
                        }
                        b.setLength(0);
                    }
                    break;
                }
                event = xpp.next();
            } while (event != XmlPullParser.END_DOCUMENT);
        } catch (XmlPullParserException e) {
            throw new ParserException(M.tr("epub.parse.invalidNcx", e.getLocalizedMessage()), e);
        }
        val extensions = book.getExtensions();
        for (val e : data.items.entrySet()) {
            item = e.getValue();
            extensions.set("opf-" + e.getKey(), Flobs.forVam(data.vam, data.opsDir + '/' + item.href, item.mime));
        }
    }

    @RequiredArgsConstructor
    private class Local {
        final Book book;
        final VdmReader vam;
        @SuppressWarnings("unused")
        final EpubInConfig config;

        String opfPath;

        String opsDir;

        // OPF scheme
        String scheme;

        // OPF role
        String role;

        // OPF event
        String event;

        String tocId;

        Map<String, Item> items = new HashMap<>();
    }

    @RequiredArgsConstructor
    private class Item {
        final String href;
        final String mime;
    }

}
