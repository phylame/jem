package pw.phylame.jem.formats.epub;

import static pw.phylame.jem.epm.util.xml.XmlUtils.attributeOf;
import static pw.phylame.jem.epm.util.xml.XmlUtils.newPullParser;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import lombok.RequiredArgsConstructor;
import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.epm.impl.VamParser;
import pw.phylame.jem.epm.util.ParserException;
import pw.phylame.jem.epm.util.VamUtils;
import pw.phylame.jem.formats.util.M;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.jem.util.text.Texts;
import pw.phylame.ycl.io.PathUtils;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.DateUtils;
import pw.phylame.ycl.util.MiscUtils;
import pw.phylame.ycl.util.StringUtils;
import pw.phylame.ycl.vam.VamReader;

public class EpubParser extends VamParser<EpubInConfig> {
    private static final String TAG = EpubParser.class.getSimpleName();

    public EpubParser() {
        super("epub", EpubInConfig.class);
    }

    @Override
    protected Book parse(VamReader vam, EpubInConfig config) throws IOException, ParserException {
        if (!EPUB.MT_EPUB.equals(VamUtils.textOf(vam, EPUB.MIME_FILE, "ASCII"))) {
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
        try (val in = VamUtils.streamOf(data.vam, EPUB.CONTAINER_FILE)) {
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
        if (StringUtils.isEmpty(data.opfPath)) {
            throw new ParserException(M.tr("epub.parse.noOpf"));
        }
        boolean hasText = false;
        val b = new StringBuilder();
        val xpp = newPullParser(false);
        try (val in = VamUtils.streamOf(data.vam, data.opfPath)) {
            xpp.setInput(in, null);
            int event = xpp.getEventType();
            do {
                val name = xpp.getName();
                switch (event) {
                case XmlPullParser.START_TAG: {
                    hasText = false;
                    if (name.equals("item")) {
                        val id = attributeOf(xpp, "id");
                        data.items.put(id, new Item(id, attributeOf(xpp, "href"), attributeOf(xpp, "media-type")));
                    } else if (name.equals("itemref")) {
                        // ignored
                    } else if (name.equals("reference")) {
                        // ignored
                    } else if (name.equals("meta")) {
                        val mName = xpp.getAttributeValue(null, "name");
                        val value = xpp.getAttributeValue(null, "content");
                        if ("cover".equals(mName)) {
                            data.coverId = value;
                        } else if (value != null) {
                            data.book.getAttributes().set(mName, value);
                        }
                    } else if (name.startsWith("dc:")) {
                        data.scheme = xpp.getAttributeValue(null, "opf:scheme");
                        data.role = xpp.getAttributeValue(null, "opf:role");
                        data.event = xpp.getAttributeValue(null, "opf:event");
                        hasText = true;
                    } else if (name.equals("package")) {
                        val version = attributeOf(xpp, "version");
                        if (!version.startsWith("2")) {
                            throw new ParserException(M.tr("epub.make.unsupportedVersion", version));
                        }
                    } else if (name.equals("spine")) {
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
                    if (name.startsWith("dc:")) {
                        parseMetadata(name.substring(3), xpp, b, data);
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
        if (StringUtils.isNotEmpty(data.coverId)) {
            val item = data.items.get(data.coverId);
            if (item != null) {
                Attributes.setCover(data.book, Flobs.forVam(data.vam, data.opsDir + '/' + item.href, null));
            }
        }
    }

    private void parseMetadata(String name, XmlPullParser xpp, StringBuilder b, Local data) {
        val book = data.book;
        val text = StringUtils.trimmed(b.toString());
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
            if (data.role.equals("aut")) {
                name = Attributes.AUTHOR;
            }
        }
        break;
        case "date": {
            if (data.equals("creation")) {
                name = Attributes.PUBDATE;
            } else if (data.event.equals("modification")) {
                name = Attributes.DATE;
            }
            try {
                value = DateUtils.parse(text, "yyyy-m-D");
            } catch (ParseException e) {
                Log.d(TAG, e);
                return;
            }
        }
        break;
        case "contributor": {
            if (data.role.equals("bkp")) {
                name = Attributes.VENDOR;
            }
        }
        break;
        case "type": {
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
        if (StringUtils.isEmpty(data.tocId)) {
            Log.d(TAG, "no toc resource found");
            return;
        }
        val item = data.items.get(data.tocId);
        if (item == null) {
            Log.d(TAG, "no toc resource found for id: {0}", data.tocId);
            return;
        }
        boolean hasText = false;
        val book = data.book;
        val b = new StringBuilder();
        val xpp = newPullParser(false);
        try (val in = VamUtils.streamOf(data.vam, data.opsDir + '/' + item.href)) {
            xpp.setInput(in, null);
            int event = xpp.getEventType();
            do {
                val name = xpp.getName();
                switch (event) {
                case XmlPullParser.START_TAG: {
                    hasText = false;
                    if (name.equals("meta")) {
                        book.getExtensions().set(attributeOf(xpp, "name").replace("dtb:", ""),
                                attributeOf(xpp, "content"));
                    } else if (name.equals("navLabel")) {
                        data.tagName = "navLabel";
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
                    if (name.equals("text")) {
                        if ("navLabel".equals(data.tagName)) {

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
    }

    private class Local {
        Book book;
        VamReader vam;
        EpubInConfig config;

        Local(Book book, VamReader vam, EpubInConfig config) {
            this.book = book;
            this.vam = vam;
            this.config = config;
        }

        String opfPath;

        String opsDir;

        // OPF scheme
        String scheme;

        // OPF role
        String role;

        // OPF event
        String event;

        String coverId;

        String tocId;

        String tagName;

        Map<String, Item> items = new HashMap<>();

        // current chapter
        Chapter chapter;

        Chapter addChapter() {
            if (chapter == null) {
                chapter = book;
            }
            val ch = new Chapter();
            chapter.append(ch);
            return ch;
        }
    }

    @RequiredArgsConstructor
    private class Item {
        final String id;
        final String href;
        final String mime;
    }

}
